package com.docuagent.localapp.ai.generator;

import com.docuagent.localapp.ai.AiGenerationRequest;
import com.docuagent.localapp.ai.AiGenerationResult;
import com.docuagent.localapp.exception.AiProviderException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiAiGenerationClient implements AiGenerationClient {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GEMINI_GENERATE_PATH = "/v1beta/models/{model}:generateContent";
    private static final int ERROR_BODY_LOG_LIMIT = 700;
    private static final Logger log = LoggerFactory.getLogger(GeminiAiGenerationClient.class);

    private final StructuredAiJsonParser structuredAiJsonParser;

    public GeminiAiGenerationClient(StructuredAiJsonParser structuredAiJsonParser) {
        this.structuredAiJsonParser = structuredAiJsonParser;
    }

    @Override
    public AiProviderType providerType() {
        return AiProviderType.GEMINI;
    }

    @Override
    public AiGenerationResult generate(AiGenerationRequest request, AiGenerationOptions options) {
        GeminiGenerateResponse response = callGemini(request, options);
        String responseText = extractResponseText(response);
        if (!StringUtils.hasText(responseText)) {
            throw new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini가 비어 있는 응답을 반환했습니다.");
        }

        return new AiGenerationResult(structuredAiJsonParser.parse(responseText, request.fields()));
    }

    private GeminiGenerateResponse callGemini(AiGenerationRequest request, AiGenerationOptions options) {
        if (!StringUtils.hasText(options.geminiApiKey())) {
            throw new AiProviderException(HttpStatus.BAD_REQUEST, "Gemini API 키를 입력해주세요.");
        }

        String model = normalizeModelName(options.geminiModel());
        log.info("Calling Gemini provider. model={}, endpoint={}{}", model, GEMINI_BASE_URL, GEMINI_GENERATE_PATH);

        RestClient restClient = RestClient.builder()
                .baseUrl(GEMINI_BASE_URL)
                .requestFactory(requestFactory(options.requestTimeoutSeconds()))
                .build();

        GeminiGenerateRequest body = new GeminiGenerateRequest(
                List.of(new GeminiContent(
                        List.of(new GeminiPart(request.systemPrompt() + "\n\n" + request.userPrompt()))
                )),
                new GeminiGenerationConfig(0.0, "application/json")
        );

        try {
            return restClient.post()
                    .uri(GEMINI_GENERATE_PATH, model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", options.geminiApiKey())
                    .body(body)
                    .retrieve()
                    .body(GeminiGenerateResponse.class);
        } catch (ResourceAccessException exception) {
            log.warn("Gemini network failure. model={}, rootCause={}, message={}",
                    model,
                    rootCauseType(exception),
                    safeRootCauseMessage(exception)
            );
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw new AiProviderException(HttpStatus.GATEWAY_TIMEOUT, "Gemini 요청 시간이 초과되었습니다.");
            }
            if (hasCause(exception, UnknownHostException.class)) {
                throw new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini API 주소를 찾지 못했습니다. 인터넷 연결 또는 DNS 설정을 확인해주세요.");
            }
            if (hasCause(exception, ConnectException.class)
                    || hasCause(exception, NoRouteToHostException.class)
                    || hasCause(exception, SocketException.class)
                    || hasCause(exception, SSLException.class)) {
                throw new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini API에 연결하지 못했습니다. 인터넷 연결 또는 방화벽 설정을 확인해주세요.");
            }
            throw new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini API에 연결하지 못했습니다. 네트워크 상태를 확인해주세요.");
        } catch (RestClientResponseException exception) {
            throw toGeminiException(exception, model);
        } catch (RestClientException exception) {
            log.warn("Gemini client failure. model={}, exception={}, message={}",
                    model,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            throw new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini 응답을 처리하지 못했습니다.");
        }
    }

    private AiProviderException toGeminiException(RestClientResponseException exception, String model) {
        String body = exception.getResponseBodyAsString();
        String lowerBody = body == null ? "" : body.toLowerCase(Locale.ROOT);
        int statusCode = exception.getStatusCode().value();
        log.warn("Gemini HTTP failure. model={}, status={}, responseBody={}",
                model,
                statusCode,
                truncate(body)
        );

        if (statusCode == 400 && lowerBody.contains("api key")) {
            return new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini API 키를 확인해주세요.");
        }
        if (statusCode == 401 || statusCode == 403 || lowerBody.contains("permission_denied")) {
            return new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini API 키 권한을 확인해주세요.");
        }
        if (statusCode == 429 || lowerBody.contains("quota") || lowerBody.contains("resource_exhausted")) {
            return new AiProviderException(HttpStatus.TOO_MANY_REQUESTS, "Gemini 사용량 한도를 초과했습니다.");
        }
        if (statusCode == 404 || lowerBody.contains("model not found") || lowerBody.contains("not found")) {
            return new AiProviderException(HttpStatus.BAD_GATEWAY, "설정된 Gemini 모델을 찾을 수 없습니다.");
        }
        if (statusCode == 400 || lowerBody.contains("invalid_argument")) {
            return new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini 요청 형식이 올바르지 않습니다. 모델 이름과 설정을 확인해주세요.");
        }
        if (statusCode >= 500) {
            return new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini 서비스가 일시적으로 응답하지 않습니다.");
        }

        return new AiProviderException(HttpStatus.BAD_GATEWAY, "Gemini 요청이 실패했습니다. API 키와 모델 이름을 확인해주세요.");
    }

    private String extractResponseText(GeminiGenerateResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return "";
        }

        GeminiCandidate candidate = response.candidates().get(0);
        if (candidate == null || candidate.content() == null || candidate.content().parts() == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (GeminiPart part : candidate.content().parts()) {
            if (part != null && StringUtils.hasText(part.text())) {
                builder.append(part.text());
            }
        }
        return builder.toString();
    }

    private SimpleClientHttpRequestFactory requestFactory(int timeoutSeconds) {
        int timeoutMillis = (int) Duration.ofSeconds(timeoutSeconds).toMillis();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }

    private String normalizeModelName(String model) {
        String normalized = StringUtils.hasText(model) ? model.trim() : "gemini-2.5-flash";
        return normalized.startsWith("models/") ? normalized.substring("models/".length()) : normalized;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String rootCauseType(Throwable throwable) {
        Throwable rootCause = rootCause(throwable);
        return rootCause == null ? throwable.getClass().getSimpleName() : rootCause.getClass().getSimpleName();
    }

    private String safeRootCauseMessage(Throwable throwable) {
        Throwable rootCause = rootCause(throwable);
        String message = rootCause == null ? throwable.getMessage() : rootCause.getMessage();
        return truncate(message);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        Throwable previous = null;
        while (current != null) {
            previous = current;
            current = current.getCause();
        }
        return previous;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replaceAll("\\s+", " ").trim();
        if (sanitized.length() <= ERROR_BODY_LOG_LIMIT) {
            return sanitized;
        }
        return sanitized.substring(0, ERROR_BODY_LOG_LIMIT) + "...";
    }

    private record GeminiGenerateRequest(
            List<GeminiContent> contents,
            GeminiGenerationConfig generationConfig
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiGenerateResponse(
            List<GeminiCandidate> candidates
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiCandidate(
            GeminiContent content,
            String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiContent(
            List<GeminiPart> parts
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiPart(
            String text
    ) {
    }

    private record GeminiGenerationConfig(
            Double temperature,
            String responseMimeType
    ) {
    }
}
