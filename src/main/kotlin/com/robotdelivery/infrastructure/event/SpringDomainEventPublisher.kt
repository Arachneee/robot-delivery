package com.robotdelivery.infrastructure.event

import com.robotdelivery.domain.common.DomainEvent
import com.robotdelivery.domain.common.DomainEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : DomainEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publishAll(events: List<DomainEvent>) {
        log.info("도메인 이벤트 발행 시작: eventCount={}", events.size)
        events.forEach { event ->
            log.info("도메인 이벤트 발행: eventType={}, eventId={}", event.javaClass.simpleName, event.eventId)
            applicationEventPublisher.publishEvent(event)
        }
    }
}
