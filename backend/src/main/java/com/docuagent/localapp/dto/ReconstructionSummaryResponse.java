package com.docuagent.localapp.dto;

import java.util.List;

public record ReconstructionSummaryResponse(
        Long taskId,
        Long documentId,
        String generatedFilePath,
        Integer totalOperations,
        Integer writtenCount,
        Integer skippedCount,
        Boolean available,
        List<ReconstructionResultResponse> results
) {

    public static ReconstructionSummaryResponse empty(Long taskId) {
        return new ReconstructionSummaryResponse(
                taskId,
                null,
                null,
                0,
                0,
                0,
                false,
                List.of()
        );
    }
}
