package com.progressoft.clusterdata.dto;

import lombok.Builder;

@Builder
public record DealImportResponse(
        int totalReceived,
        int successfulImports,
        int failedOrSkipped
) {
}
