package com.synapse.payment_service.exception;

public class PortOneClientException extends PaymentException {
    public PortOneClientException(ExceptionCode exceptionCode) {
        super(exceptionCode);
    }
}
