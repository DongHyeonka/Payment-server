package com.synapse.payment_service.eventuate.messaging;

import java.util.UUID;

import com.synapse.account_service_api.aggregate.AggregateType;
import com.synapse.account_service_api.event.SubscriptionCreatedEvent;
import com.synapse.payment_service.service.eventuate.SubscriptionService;

import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SubscriptionEventConsumer {
    private final SubscriptionService subscriptionService;

    public DomainEventHandlers domainEventHandlers() {
        return DomainEventHandlersBuilder
                .forAggregateType(AggregateType.SUBSCRIPTION.getAggregateType())
                .onEvent(SubscriptionCreatedEvent.class, this::createSubscription)
                .build();
    }

    private void createSubscription(DomainEventEnvelope<SubscriptionCreatedEvent> de) {
        String subscriptionIds = de.getAggregateId();

        UUID subscriptionId = UUID.fromString(subscriptionIds);
        UUID userId = de.getEvent().getUserId();
        String subscriptionTier = de.getEvent().getSubscriptionTier();
        // int maxSubscriptionCount = de.getEvent().getMaxSubscriptionCount(); 이 값은 필요 없을듯..?
        String nextRenewalDate = de.getEvent().getNextRenewalDate();
        
        subscriptionService.createdSubscription(subscriptionId, userId, subscriptionTier, nextRenewalDate);
    }
}
