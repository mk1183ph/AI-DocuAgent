package com.docuagent.localapp.ai.generator;

import com.docuagent.localapp.exception.BadRequestException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AiGenerationClientResolver {

    private final Map<AiProviderType, AiGenerationClient> clients = new EnumMap<>(AiProviderType.class);

    public AiGenerationClientResolver(List<AiGenerationClient> clients) {
        clients.forEach(client -> this.clients.put(client.providerType(), client));
    }

    public AiGenerationClient resolve(AiProviderType providerType) {
        AiGenerationClient client = clients.get(providerType);
        if (client == null) {
            throw new BadRequestException("지원하지 않는 AI 제공자입니다: " + providerType);
        }
        return client;
    }
}
