package com.progressoft.clusterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Builder
public record DealRequest (

        @NotBlank(message = "Deal Unique Id is required")
        String dealUniqueId,

        @NotBlank(message = "From Currency is required")
        @Size(min = 3, max = 3, message = "ISO code must be 3 characters")
        String fromCurrency,

        @NotBlank(message = "To Currency is required")
        @Size(min = 3, max = 3, message = "ISO code must be 3 characters")
        String toCurrency,

        @NotNull(message = "Deal timestamp is required")
        LocalDateTime dealTimestamp,

        @NotNull(message = "Deal amount is required")
        @Positive(message = "Deal amount must be positive")
        BigDecimal dealAmount)
{}
