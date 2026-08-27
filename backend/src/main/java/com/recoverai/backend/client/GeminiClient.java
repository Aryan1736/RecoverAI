package com.recoverai.backend.client;

import com.recoverai.backend.dto.diagnosis.DiagnosisContext;
import com.recoverai.backend.dto.diagnosis.StructuredDiagnosisResponse;

public interface GeminiClient {

    /**
     * Sends the structured failure diagnosis context to Gemini and returns a validated diagnosis response.
     *
     * @param context Sanitized diagnostic context representing the failed payment and recovery case
     * @return StructuredDiagnosisResponse with recommendation, channel, score, reasoning, factors, and token metadata
     */
    StructuredDiagnosisResponse diagnose(DiagnosisContext context);
}
