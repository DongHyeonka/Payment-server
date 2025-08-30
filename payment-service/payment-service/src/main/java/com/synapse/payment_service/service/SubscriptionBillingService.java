package com.synapse.payment_service.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.entity.Subscription;
import com.synapse.payment_service.service.persistence.db.PaymentServiceSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionBillingService {
    private final PaymentServiceSubscriptionRepository paymentServiceSubscriptionRepository;
    private final SubscriptionBillingWorker subscriptionBillingWorker;
    private final ExecutorService billingExecutor = Executors.newFixedThreadPool(10);

    @Transactional
    public void processDailySubscriptions() {
        // 오늘이 다음 결제일인 모든 활성 구독을 찾는다.
        LocalDate today = LocalDate.now();
        ZonedDateTime startOfDay = today.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault());
        final int PAGE_SIZE = 100;

        Slice<Subscription> targetSlice = paymentServiceSubscriptionRepository.findActiveSubscriptionsDueForRenewal(
            startOfDay, endOfDay, PageRequest.of(0, PAGE_SIZE)
        );

        if (targetSlice.isEmpty()) {
            log.info("오늘 결제 대상인 구독이 없습니다.");
            return;
        }

        do {
            List<Subscription> subscriptionsInPage = targetSlice.getContent();

            List<CompletableFuture<Void>> futures = subscriptionsInPage.stream()
                    .map(subscription -> CompletableFuture.runAsync(
                        () -> subscriptionBillingWorker.chargeAndRenewSubscription(subscription), billingExecutor)
                    )
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("페이지 처리가 완료되었습니다.");

            if (targetSlice.hasNext()) {
                targetSlice = paymentServiceSubscriptionRepository.findActiveSubscriptionsDueForRenewal(
                    startOfDay, endOfDay, targetSlice.nextPageable()
                );
            } else {
                break;
            }
        } while (true);
    }

    @Transactional
    public long processDailyExpiredSubscriptions() {
        final int PAGE_SIZE = 100;
        long totalProcessedCount = 0;

        Slice<Subscription> expiredSubscriptions = paymentServiceSubscriptionRepository
                .findExpiredSubscriptionsWithCursor(
                        PageRequest.of(0, PAGE_SIZE));

        if (expiredSubscriptions.isEmpty()) {
            log.info("만료 처리할 구독이 없습니다.");
            return totalProcessedCount;
        }

        do {
            List<Subscription> subscriptions = expiredSubscriptions.getContent();

            subscriptions.forEach(Subscription::expireSubscription); // 커서 기반 조회를 통해서 가져오기에 따로 Map 자료구조가 아닌 forEach로도 충분함

            totalProcessedCount += subscriptions.size();

            if (expiredSubscriptions.hasNext()) {
                expiredSubscriptions = paymentServiceSubscriptionRepository.findExpiredSubscriptionsWithCursor(
                        expiredSubscriptions.nextPageable());
            } else {
                break;
            }
        } while (true);

        return totalProcessedCount;
    }
}
