package com.progressoft.clusterdata.dto;

public record DealImportResponse(
        int totalReceived,
        int successfulImports,
        int failedOrSkipped
) {
}
