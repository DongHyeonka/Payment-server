package com.synapse.payment_service.dto.response;

import java.math.BigDecimal;

public record PaymentPreparationResponse(
    String merchantUid,
    String orderName,
    BigDecimal amount
) {
    
}
