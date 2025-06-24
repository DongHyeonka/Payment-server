package com.synapse.payment_service.dto;

public record PortOneAuthResponse(
    String accessToken,
    String refreshToken
) {
    
}
