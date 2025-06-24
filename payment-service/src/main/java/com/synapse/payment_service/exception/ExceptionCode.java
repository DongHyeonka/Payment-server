package com.synapse.payment_service.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum ExceptionCode {
    PORT_ONE_CLIENT_ERROR(INTERNAL_SERVER_ERROR, "P001", "포트원 클라이언트 오류"),
    SUBSCRIPTION_NOT_FOUND(NOT_FOUND, "P002", "구독 정보를 찾을 수 없습니다")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
