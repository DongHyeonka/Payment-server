package com.synapse.payment_service.eventuate.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.synapse.account_service_api.dispatcher.DisPatcherType;
import com.synapse.payment_service.eventuate.messaging.MemberEventConsumer;
import com.synapse.payment_service.eventuate.messaging.SubscriptionEventConsumer;
import com.synapse.payment_service.service.eventuate.MemberService;
import com.synapse.payment_service.service.eventuate.SubscriptionService;

import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import io.eventuate.tram.spring.events.subscriber.TramEventSubscriberConfiguration;

/**
 * consumer에 관한 설정 + publisher에 관한 설정도 같이 있음
 */
@Configuration
@Import({ PaymentServiceWithRepositoriesConfiguration.class, TramEventSubscriberConfiguration.class })
public class PaymentServiceMessagingConfiguration {
    @Bean
    public MemberEventConsumer memberEventConsumer(MemberService memberService) {
        return new MemberEventConsumer(memberService);
    }

    @Bean
    public SubscriptionEventConsumer subscriptionEventConsumer(SubscriptionService subscriptionService) {
        return new SubscriptionEventConsumer(subscriptionService);
    }

    @Bean
    public DomainEventDispatcher memberEventDispatcher(
        MemberEventConsumer memberEventConsumer,
        DomainEventDispatcherFactory domainEventDispatcherFactory
    ) {
        return domainEventDispatcherFactory.make(DisPatcherType.MEMBER.getDispatcherType(), memberEventConsumer.domainEventHandlers()); // member 관련 이벤트를 처리하는 컨슈머 그룹룹
    }

    @Bean
    public DomainEventDispatcher subscriptionEventDispatcher(
        SubscriptionEventConsumer subscriptionEventConsumer,
        DomainEventDispatcherFactory domainEventDispatcherFactory
    ) {
        return domainEventDispatcherFactory.make(DisPatcherType.SUBSCRIPTION.getDispatcherType(), subscriptionEventConsumer.domainEventHandlers()); // subscription 관련 이벤트를 처리하는 컨슈머 그룹룹
    }
}
