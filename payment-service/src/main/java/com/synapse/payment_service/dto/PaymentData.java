package com.synapse.payment_service.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentData(
    BigDecimal amount,

    // 결제 성공/실패/취소 여부를 판단하기 위한 상태
    String status,

    // 우리 시스템의 주문 정보와 매칭하기 위한 주문번호
    @JsonProperty("merchant_uid") String merchantUid,

    // 실패 시 원인 파악을 위한 필드
    @JsonProperty("fail_reason") String failReason,

    // 고객에게 보여줄 결제 수단 정보
    @JsonProperty("card_name") String cardName,

    @JsonProperty("card_number") String cardNumber,

    //결제 완료 시각
    ZonedDateTime paidAt
) {
    
}
