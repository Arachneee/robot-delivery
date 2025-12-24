package com.robotdelivery.config

import com.robotdelivery.domain.delivery.event.DeliveryApproachingEvent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@TestConfiguration
class TestEventListenerConfig {
    @Bean
    fun testDeliveryApproachingEventListener(): TestDeliveryApproachingEventListener = TestDeliveryApproachingEventListener()
}

@Component
class TestDeliveryApproachingEventListener {
    private val capturedEvents = mutableListOf<DeliveryApproachingEvent>()

    @EventListener
    fun handle(event: DeliveryApproachingEvent) {
        capturedEvents.add(event)
    }

    fun getCapturedEvents(): List<DeliveryApproachingEvent> = capturedEvents.toList()

    fun clear() {
        capturedEvents.clear()
    }

    fun getLastEvent(): DeliveryApproachingEvent? = capturedEvents.lastOrNull()
}

