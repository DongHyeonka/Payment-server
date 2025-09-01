package com.synapse.payment_service.service.externalservice;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.payment_service.domain.entity.Order;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.UnauthorizedException;
import com.synapse.payment_service.service.convert.PaymentStatusConverter;
import com.synapse.payment_service.service.persistence.db.PaymentServiceOrderRepository;
import com.synapse.payment_service_api.dto.request.PaymentWebhookRequest;

import io.portone.sdk.server.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentProcessor {
    private final PortOneService portOneService;
    private final PaymentServiceOrderRepository paymentServiceOrderRepository;
    private final PaymentStatusConverter paymentStatusConverter;

    public void processWebhook(String requestBody, ObjectMapper objectMapper) throws IOException {
        PaymentWebhookRequest webhookRequest = PaymentWebhookRequest.from(requestBody, objectMapper);
        if (!webhookRequest.isTransactionWebhook()) {
            log.info("처리 대상이 아닌 웹훅 이벤트를 수신했습니다. event={}", webhookRequest.type());
            return;
        }

        Order order = paymentServiceOrderRepository.findByOrderId(webhookRequest.getPaymentId());

        if (order.isAlreadyProcessed()) {
            log.warn("이미 최종 처리된 주문에 대한 웹훅입니다. paymentId={}", order.getPaymentId());
            return;
        }

        Payment payment = portOneService.payment(webhookRequest.getTransactionId());
        order.updateIamPortTransactionId(webhookRequest.getTransactionId());

        paymentStatusConverter.processPayment(order, payment);
    }

    public void processClientVerification(String paymentId, String iamPortTransactionId, UUID memberId) {
        Order order = paymentServiceOrderRepository.findByOrderId(paymentId);

        if (!order.getSubscription().getMember().getMemberId().equals(memberId)) {
            throw new UnauthorizedException(ExceptionCode.UNAUTHORIZED_USER);
        }

        if (order.isAlreadyProcessed()) {
            log.info("이미 처리된 결제입니다. paymentId={}", order.getPaymentId());
            return;
        }

        Payment payment = portOneService.payment(iamPortTransactionId);
        order.updateIamPortTransactionId(iamPortTransactionId);

        paymentStatusConverter.processPayment(order, payment);
    }
}
