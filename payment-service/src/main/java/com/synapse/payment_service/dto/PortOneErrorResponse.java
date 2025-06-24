package com.synapse.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneErrorResponse(
    ErrorDetails error
) {
    public record ErrorDetails(
        String code, String message
    ) {

    }
}
