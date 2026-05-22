package com.docuagent.localapp.ai.generator;

import com.docuagent.localapp.ai.AiGenerationRequest;
import com.docuagent.localapp.ai.AiGenerationResult;
import com.docuagent.localapp.exception.AiProviderException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OllamaAiGenerationClient implements AiGenerationClient {

    private final StructuredAiJsonParser structuredAiJsonParser;

    public OllamaAiGenerationClient(StructuredAiJsonParser structuredAiJsonParser) {
        this.structuredAiJsonParser = structuredAiJsonParser;
    }

    @Override
    public AiProviderType providerType() {
        return AiProviderType.OLLAMA;
    }

    @Override
    public AiGenerationResult generate(AiGenerationRequest request, AiGenerationOptions options) {
        OllamaGenerateResponse response = callOllama(request, options);
        if (response == null || response.response() == null || response.response().isBlank()) {
            throw new AiProviderException(HttpStatus.BAD_GATEWAY, "Ollama가 비어 있는 응답을 반환했습니다.");
        }

        return new AiGenerationResult(structuredAiJsonParser.parse(response.response(), request.fields()));
    }

    private OllamaGenerateResponse callOllama(AiGenerationRequest request, AiGenerationOptions options) {
        RestClient restClient = RestClient.builder()
                .baseUrl(options.ollamaBaseUrl())
                .requestFactory(requestFactory(options.requestTimeoutSeconds()))
                .build();

        OllamaGenerateRequest body = new OllamaGenerateRequest(
                options.ollamaModel(),
                request.systemPrompt() + "\n\n" + request.userPrompt(),
                false,
                "json",
                Map.of("temperature", 0)
        );

        try {
            return restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OllamaGenerateResponse.class);
        } catch (ResourceAccessException exception) {
            if (hasCause(exception, SocketTimeoutException.class)) {
                throw new AiProviderException(HttpStatus.GATEWAY_TIMEOUT, "요청 시간이 초과되었습니다.");
            }
            if (hasCause(exception, ConnectException.class)) {
                throw new AiProviderException(HttpStatus.BAD_GATEWAY, "Ollama가 실행 중인지 확인해주세요.");
            }
            throw new AiProviderException(HttpStatus.BAD_GATEWAY, "Ollama가 실행 중인지 확인해주세요.");
        } catch (RestClientResponseException exception) {
            if (isModelNotFound(exception)) {
                throw new AiProviderException(HttpStatus.BAD_GATEWAY, "설정된 모델을 찾을 수 없습니다.");
            }
            throw new AiProviderException(HttpStatus.BAD_GATEWAY,
                    "Ollama 요청이 실패했습니다. 모델 이름과 Ollama 상태를 확인해주세요.");
        } catch (RestClientException exception) {
            throw new AiProviderException(HttpStatus.BAD_GATEWAY,
                    "Ollama 응답을 처리하지 못했습니다.");
        }
    }

    private SimpleClientHttpRequestFactory requestFactory(int timeoutSeconds) {
        int timeoutMillis = (int) Duration.ofSeconds(timeoutSeconds).toMillis();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }

    private boolean isModelNotFound(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        return exception.getStatusCode().value() == 404
                || body.toLowerCase().contains("model")
                && body.toLowerCase().contains("not found");
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

    private record OllamaGenerateRequest(
            String model,
            String prompt,
            boolean stream,
            String format,
            Map<String, Object> options
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaGenerateResponse(
            String model,
            String response,
            Boolean done
    ) {
    }
}
