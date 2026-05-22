package com.docuagent.localapp.ai.generator;

import com.docuagent.localapp.ai.AiGenerationField;
import com.docuagent.localapp.ai.AiGenerationRequest;
import com.docuagent.localapp.ai.AiGenerationResult;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MockAiGenerationClient implements AiGenerationClient {

    private static final String NEEDS_INPUT = "추가 입력 필요";
    private static final String NOT_PROVIDED = "미기재";
    private static final Set<String> MATERIAL_KEYWORDS = Set.of(
            "색종이", "가위", "풀", "마커", "크레파스", "도화지", "스티커", "물감", "붓", "공", "사진", "카드"
    );
    private static final List<String> NOTE_KEYWORDS = List.of("어려움", "특이", "지원", "필요", "주의", "안전");
    private static final List<String> REACTION_KEYWORDS = List.of(
            "유아", "아동", "반응", "흥미", "참여", "어려워", "즐거", "좋아", "싫어", "집중", "말하"
    );
    private static final List<String> PLAN_KEYWORDS = List.of("지원", "계획", "필요", "추후", "계속", "연습");

    @Override
    public AiProviderType providerType() {
        return AiProviderType.MOCK;
    }

    @Override
    public AiGenerationResult generate(AiGenerationRequest request, AiGenerationOptions options) {
        Map<String, String> structuredContent = new LinkedHashMap<>();
        String userContext = normalize(request.userContext());

        for (AiGenerationField field : request.fields()) {
            structuredContent.putIfAbsent(field.fieldKey(), valueFor(field, userContext));
        }

        return new AiGenerationResult(structuredContent);
    }

    private String valueFor(AiGenerationField field, String userContext) {
        if (userContext.isBlank()) {
            return missingValue(field);
        }

        String profile = normalize(field.sourceLabel() + " " + field.displayName() + " " + field.description()).toLowerCase(Locale.ROOT);

        if (containsAny(profile, "준비", "재료", "material")) {
            return extractMaterials(userContext, field);
        }
        if (containsAny(profile, "반응", "흥미", "참여", "reaction")) {
            return sentencesContaining(userContext, REACTION_KEYWORDS, field);
        }
        if (containsAny(profile, "특이", "비고", "메모", "note")) {
            return sentencesContaining(userContext, NOTE_KEYWORDS, field);
        }
        if (containsAny(profile, "계획", "지원", "follow", "plan")) {
            return supportPlan(userContext, field);
        }
        if (containsAny(profile, "분석", "해석", "analysis")) {
            return groundedAnalysis(userContext, field);
        }
        if (containsAny(profile, "요약", "summary")) {
            return summarize(userContext);
        }

        return userContext;
    }

    private String extractMaterials(String userContext, AiGenerationField field) {
        StringBuilder materials = new StringBuilder();
        for (String keyword : MATERIAL_KEYWORDS) {
            if (userContext.contains(keyword)) {
                if (!materials.isEmpty()) {
                    materials.append(", ");
                }
                materials.append(keyword);
            }
        }

        return materials.isEmpty() ? missingValue(field) : materials.toString();
    }

    private String supportPlan(String userContext, AiGenerationField field) {
        String direct = sentencesContaining(userContext, PLAN_KEYWORDS, field);
        if (!NEEDS_INPUT.equals(direct) && !NOT_PROVIDED.equals(direct)) {
            return direct;
        }
        if (userContext.contains("어울") || userContext.contains("어려")) {
            return "입력된 사실을 바탕으로 사회적 상호작용 지원이 필요해 보입니다.";
        }
        return missingValue(field);
    }

    private String groundedAnalysis(String userContext, AiGenerationField field) {
        if (userContext.contains("어울") || userContext.contains("어려")) {
            return "입력된 사실을 바탕으로 또래 상호작용에서 지원이 필요해 보입니다.";
        }
        return sentencesContaining(userContext, NOTE_KEYWORDS, field);
    }

    private String sentencesContaining(String userContext, List<String> keywords, AiGenerationField field) {
        return Arrays.stream(userContext.split("(?<=[.!?。])\\s+|\\n+"))
                .map(String::trim)
                .filter(sentence -> !sentence.isBlank())
                .filter(sentence -> keywords.stream().anyMatch(keyword -> hasText(keyword) && sentence.contains(keyword)))
                .reduce((left, right) -> left + " " + right)
                .orElse(missingValue(field));
    }

    private String summarize(String userContext) {
        if (userContext.length() <= 220) {
            return userContext;
        }

        return userContext.substring(0, 220).trim();
    }

    private boolean containsAny(String value, String... tokens) {
        return Arrays.stream(tokens).anyMatch(value::contains);
    }

    private String missingValue(AiGenerationField field) {
        return field.requiredValue() ? NEEDS_INPUT : NOT_PROVIDED;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
