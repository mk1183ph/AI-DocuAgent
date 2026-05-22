package com.docuagent.localapp.dto;

import java.nio.file.Path;

public record ReconstructedDocumentResponse(
        Path filePath,
        String downloadFileName,
        ReconstructionSummaryResponse summary
) {
}
