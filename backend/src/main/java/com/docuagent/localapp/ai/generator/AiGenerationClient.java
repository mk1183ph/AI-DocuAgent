package com.docuagent.localapp.ai.generator;

import com.docuagent.localapp.ai.AiGenerationRequest;
import com.docuagent.localapp.ai.AiGenerationResult;

public interface AiGenerationClient {

    AiProviderType providerType();

    AiGenerationResult generate(AiGenerationRequest request, AiGenerationOptions options);
}
