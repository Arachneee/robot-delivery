package com.robotdelivery.infrastructure.event

import com.robotdelivery.domain.common.DomainEvent
import com.robotdelivery.domain.common.DomainEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : DomainEventPublisher {
    override fun publishAll(events: List<DomainEvent>) {
        events.forEach { event ->
            applicationEventPublisher.publishEvent(event)
        }
    }
}
