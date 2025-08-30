package com.synapse.payment_service.service.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.synapse.payment_service.service.SubscriptionBillingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {
    private final SubscriptionBillingService subscriptionBillingService;

    @Scheduled(cron = "0 0 4 * * *")
    public void runDailyBilling() {
        log.info("일일 정기 결제 스케줄러를 시작합니다.");
        subscriptionBillingService.processDailySubscriptions(); 
        log.info("일일 정기 결제 스케줄러를 종료합니다.");
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void expireSubscriptions() {
        log.info("구독 만료 처리 스케줄러를 시작합니다.");
        long totalProcessedCount = subscriptionBillingService.processDailyExpiredSubscriptions();
        log.info("구독 만료 처리 스케줄러를 종료합니다. 총 처리된 구독 수: {}", totalProcessedCount);
    }
}
