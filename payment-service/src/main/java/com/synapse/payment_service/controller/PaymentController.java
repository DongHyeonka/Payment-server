package com.synapse.payment_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.payment_service.dto.request.PaymentRequestDto;
import com.synapse.payment_service.dto.response.PaymentPreparationResponse;
import com.synapse.payment_service.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * 프론트엔드에서 결제창을 띄우기 전에,
     * 결제에 필요한 정보(주문번호, 금액 등)를 생성하고 반환합니다.
     */
    @PostMapping("/request")
    public ResponseEntity<PaymentPreparationResponse> requestPayment(
        @RequestBody @Valid PaymentRequestDto request,
        @AuthenticationPrincipal UUID memberId
    ) {
        PaymentPreparationResponse response = paymentService.preparePayment(memberId, request);
        return ResponseEntity.ok().body(response);
    }
    
}
