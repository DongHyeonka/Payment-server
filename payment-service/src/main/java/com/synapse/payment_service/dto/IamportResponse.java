package com.synapse.payment_service.dto;

/**
 * 아임포트 API의 공통적인 응답 래퍼(Wrapper) 클래스입니다.
 * 
 * @param <T> 실제 데이터의 타입 (예: PaymentData)
 */
public record IamportResponse<T>(
    int code,
    String message,
    T response
) {

}
