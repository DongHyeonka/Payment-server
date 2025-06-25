package com.synapse.payment_service.dto.response;

import java.math.BigDecimal;

public record PaymentPreparationResponse(
    String paymentId,
    String orderName,
    BigDecimal amount
) {
    
}
