package com.synapse.payment_service.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.Subscription;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.domain.enums.SubscriptionTier;
import com.synapse.payment_service.dto.request.PaymentRequestDto;
import com.synapse.payment_service.dto.response.PaymentPreparationResponse;
import com.synapse.payment_service.exception.ExceptionCode;
import com.synapse.payment_service.exception.NotFoundException;
import com.synapse.payment_service.repository.OrderRepository;
import com.synapse.payment_service.repository.SubscriptionRepository;

import lombok.RequiredArgsConstructor;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentPreparationResponse preparePayment(UUID memberId, PaymentRequestDto request) {
        SubscriptionTier tier = SubscriptionTier.valueOf(request.tier().toUpperCase());
        BigDecimal amount = tier.getMonthlyPrice();
        String orderName = tier.getTierName() + "_" + "구독";
        String merchantUid = orderName + "_" + UUID.randomUUID();

        Subscription subscription = subscriptionRepository.findByMemberId(memberId)
            .orElseThrow(() -> new NotFoundException(ExceptionCode.SUBSCRIPTION_NOT_FOUND));

        Order order = Order.builder()
                .subscription(subscription)
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        orderRepository.save(order);

        return new PaymentPreparationResponse(merchantUid, orderName, amount);
    }


}
