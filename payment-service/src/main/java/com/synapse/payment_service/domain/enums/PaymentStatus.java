package com.synapse.payment_service.domain.enums;

public enum PaymentStatus {
    PAID, // 결제 완료
    FAILED, // 결제 실패
    CANCELLED, // 결제 취소 (환불)
    PENDING // 결제 대기
}
