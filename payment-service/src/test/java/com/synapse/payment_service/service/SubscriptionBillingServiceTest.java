package com.synapse.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.config.PortOneClientProperties;
import com.synapse.payment_service.domain.Order;
import com.synapse.payment_service.domain.Subscription;
import com.synapse.payment_service.domain.enums.PaymentStatus;
import com.synapse.payment_service.domain.enums.SubscriptionStatus;
import com.synapse.payment_service.domain.enums.SubscriptionTier;
import com.synapse.payment_service.repository.OrderRepository;
import com.synapse.payment_service.repository.SubscriptionRepository;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.BillingKeyPaymentSummary;
import io.portone.sdk.server.payment.PayWithBillingKeyResponse;

@ExtendWith(MockitoExtension.class)
public class SubscriptionBillingServiceTest {
    @InjectMocks
    private SubscriptionBillingService subscriptionBillingService;
    
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PortOneClient portoneClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private PortOneClientProperties portOneClientProperties;

    @Test
    @DisplayName("정기 결제 성공: 만료일이 된 구독에 대해 빌링키 결제를 성공하고 상태를 갱신한다")
    void processDailySubscriptions_success() {
        // given
        Subscription mockSubscription = mock(Subscription.class);
        given(mockSubscription.getBillingKey()).willReturn("test-billing-key");
        given(mockSubscription.getTier()).willReturn(SubscriptionTier.PRO);
        given(mockSubscription.getId()).willReturn(1L);
        
        List<Subscription> targets = List.of(mockSubscription);
        given(subscriptionRepository.findActiveSubscriptionsDueForRenewal(any(ZonedDateTime.class), any(ZonedDateTime.class))).willReturn(targets);
        
        // PortOne SDK의 응답을 모의 처리
        PayWithBillingKeyResponse mockResponse = mock(PayWithBillingKeyResponse.class);
        BillingKeyPaymentSummary mockPaymentResult = mock(BillingKeyPaymentSummary.class);
        
        given(portoneClient.getPayment()).willReturn(paymentClient);
        given(portOneClientProperties.channelKey()).willReturn("test-channel-key");
        
        // CompletableFuture가 성공적으로 완료되도록 설정
        CompletableFuture<PayWithBillingKeyResponse> successFuture = CompletableFuture.completedFuture(mockResponse);
        
        // payWithBillingKey 호출이 성공하는 CompletableFuture를 반환하도록 설정
        given(paymentClient.payWithBillingKey(
            anyString(), anyString(), anyString(), anyString(),
            any(), any(), any(PaymentAmountInput.class), any(),
            any(), any(), any(), any(),
            any(), any(), any(), any(),
            any(), any(), any(), any(), any()
        )).willReturn(successFuture);
        
        // mock 응답 설정 - 모든 필요한 값들을 완벽하게 설정
        given(mockResponse.getPayment()).willReturn(mockPaymentResult);
        given(mockPaymentResult.getPgTxId()).willReturn("test-pg-tx-id");

        // when
        subscriptionBillingService.processDailySubscriptions();

        // then
        // 성공 핸들러가 호출되어 renewSubscription이 호출되었는지 확인
        verify(mockSubscription, times(1)).renewSubscription(SubscriptionTier.PRO);
        
        // Order가 2번 저장되었는지 확인 (PENDING -> PAID)
        verify(orderRepository, times(2)).save(any(Order.class));
        
        // 실패 핸들러는 호출되지 않았음을 확인
        verify(mockSubscription, never()).handlePaymentFailure();
        verify(subscriptionRepository, never()).save(mockSubscription);
    }
    
    @Test
    @DisplayName("정기 결제 실패: PortOne API 호출이 실패하면, 실패 주문을 저장하고 구독 상태를 PAYMENT_FAILED로 변경한다")
    void processDailySubscriptions_fail() {
        // given
        Subscription mockSubscription = mock(Subscription.class);
        given(mockSubscription.getBillingKey()).willReturn("test-billing-key");
        given(mockSubscription.getTier()).willReturn(SubscriptionTier.PRO);
        given(mockSubscription.getId()).willReturn(1L);

        List<Subscription> targets = List.of(mockSubscription);
        given(subscriptionRepository.findActiveSubscriptionsDueForRenewal(any(ZonedDateTime.class), any(ZonedDateTime.class))).willReturn(targets);
        
        // PortOne SDK가 예외를 던지는 상황을 모의 처리
        given(portoneClient.getPayment()).willReturn(paymentClient);
        given(portOneClientProperties.channelKey()).willReturn("test-channel-key");
        given(paymentClient.payWithBillingKey(
            anyString(), anyString(), anyString(), anyString(),
            any(), anyString(), any(PaymentAmountInput.class), any(),
            any(), any(), any(), any(),
            any(), any(), any(), any(),
            any(), any(), anyString(), any(), any()
        )).willReturn(CompletableFuture.failedFuture(new RuntimeException("PG사 연동 실패")));

        // when
        subscriptionBillingService.processDailySubscriptions();

        // then
        // Order가 2번 저장되었는지 확인 (PENDING -> FAILED)
        verify(orderRepository, times(2)).save(any(Order.class));
        
        // handlePaymentFailure() 메서드가 호출되었는지 검증
        verify(mockSubscription, times(1)).handlePaymentFailure();
        
        // 변경된 subscription이 저장되었는지 검증
        verify(subscriptionRepository, times(1)).save(mockSubscription);
        
        // renewSubscription은 호출되지 않았음을 검증 (실패했으므로)
         verify(mockSubscription, never()).renewSubscription(any(SubscriptionTier.class));
     }
}

/**
 * 실제 데이터베이스를 사용하는 통합 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubscriptionBillingServiceIntegrationTest {
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    @DisplayName("통합 테스트: failureHandler 호출 시 구독 상태가 PAYMENT_FAILED로 변경되고 retryCount가 1 증가한다")
    void failureHandler_integrationTest_shouldUpdateSubscriptionStatusAndRetryCount() {
        // given
        UUID memberId = UUID.randomUUID();
        Subscription subscription = Subscription.builder()
                .memberId(memberId)
                .tier(SubscriptionTier.PRO)
                .remainingChatCredits(SubscriptionTier.PRO.getMaxRequestCount())
                .status(SubscriptionStatus.ACTIVE)
                .expiresAt(ZonedDateTime.now().plusDays(30))
                .build();
        
        // billingKey 설정
        subscription.updateBillingKey("test-billing-key");
        
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        
        // 초기 상태 확인
        assertThat(savedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(savedSubscription.getRetryCount()).isEqualTo(0);
        
        // when - failureHandler 로직을 직접 실행
        String paymentId = "test-payment-id";
        
        // 결제 실패 정보 저장 (failureHandler 로직과 동일)
        Order order = Order.builder()
                .subscription(savedSubscription)
                .paymentId(paymentId)
                .amount(savedSubscription.getTier().getMonthlyPrice())
                .status(PaymentStatus.FAILED)
                .build();
        orderRepository.save(order);
        
        // 구독 상태를 PAYMENT_FAILED로 변경하고 retryCount 증가 (failureHandler 로직과 동일)
        savedSubscription.handlePaymentFailure();
        subscriptionRepository.save(savedSubscription);
        
        // then
        // 데이터베이스에서 다시 조회하여 상태 확인
        Subscription updatedSubscription = subscriptionRepository.findById(savedSubscription.getId()).orElseThrow();
        
        // 구독 상태가 PAYMENT_FAILED로 변경되었는지 확인
        assertThat(updatedSubscription.getStatus()).isEqualTo(SubscriptionStatus.PAYMENT_FAILED);
        
        // retryCount가 1 증가했는지 확인
        assertThat(updatedSubscription.getRetryCount()).isEqualTo(1);
        
        // 실패한 Order가 저장되었는지 확인
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        
        Order failedOrder = orders.get(0);
        assertThat(failedOrder.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedOrder.getSubscription().getId()).isEqualTo(savedSubscription.getId());
        assertThat(failedOrder.getAmount()).isEqualTo(SubscriptionTier.PRO.getMonthlyPrice());
        assertThat(failedOrder.getPaymentId()).isEqualTo(paymentId);
    }
}
