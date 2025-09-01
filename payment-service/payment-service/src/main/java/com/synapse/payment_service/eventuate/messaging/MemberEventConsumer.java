package com.synapse.payment_service.eventuate.messaging;

import java.util.UUID;

import com.synapse.account_service_api.aggregate.AggregateType;
import com.synapse.account_service_api.event.MemberRegisteredEvent;
import com.synapse.payment_service.service.eventuate.MemberService;

import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberEventConsumer {
    private final MemberService memberService;

    public DomainEventHandlers domainEventHandlers() {
        return DomainEventHandlersBuilder
                .forAggregateType(AggregateType.MEMBER.getAggregateType())
                .onEvent(MemberRegisteredEvent.class, this::registerMember)
                .build();
    }

    private void registerMember(DomainEventEnvelope<MemberRegisteredEvent> de) {
        String memberIds = de.getAggregateId();
        UUID memberId = UUID.fromString(memberIds);
        String email = de.getEvent().getEmail();
        String userName = de.getEvent().getUsername();
        memberService.createdMember(memberId, email, userName);
    }
}
