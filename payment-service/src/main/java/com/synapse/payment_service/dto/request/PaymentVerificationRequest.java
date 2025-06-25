package com.synapse.payment_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentVerificationRequest(
    @NotBlank(message = "imp_uid는 필수입니다")
    String impUid, 
    
    @NotBlank(message = "merchant_uid는 필수입니다")
    String merchantUid
) {
    
}
