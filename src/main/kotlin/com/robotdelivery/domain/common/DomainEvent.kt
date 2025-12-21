package com.robotdelivery.domain.common

import jakarta.persistence.Transient
import java.time.LocalDateTime
import java.util.UUID

abstract class DomainEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

interface DomainEventPublisher {
    fun publishAll(events: List<DomainEvent>)
}

abstract class AggregateRoot {
    @Transient
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()

    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    fun pullDomainEvents(): List<DomainEvent> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }
}
