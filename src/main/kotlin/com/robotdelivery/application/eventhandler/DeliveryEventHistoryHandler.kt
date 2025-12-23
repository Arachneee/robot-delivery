package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.delivery.event.DeliveryEvent
import com.robotdelivery.domain.delivery.event.DeliveryEventHistory
import com.robotdelivery.domain.delivery.event.DeliveryEventHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DeliveryEventHistoryHandler(
    private val deliveryEventHistoryRepository: DeliveryEventHistoryRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    fun handle(event: DeliveryEvent) {
        log.info("DeliveryEvent 수신: eventType={}", event::class.simpleName)

        val history = DeliveryEventHistory.from(event)
        deliveryEventHistoryRepository.save(history)

        log.info(
            "DeliveryEventHistory 저장 완료: eventName={}, deliveryId={}, robotId={}",
            history.eventType,
            history.deliveryId,
            history.robotId,
        )
    }
}
