package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.event.RobotApproachingEvent
import com.robotdelivery.domain.robot.event.RobotArrivedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DeliveryRobotEventHandler(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    fun handleApproaching(event: RobotApproachingEvent) {
        log.info("RobotApproachingEvent 수신: robotId={}, destination={}", event.robotId, event.destination)

        val delivery = findDeliveryByRobotId(event.robotId) ?: return

        delivery.approaching()
        deliveryRepository.save(delivery)

        log.info("DeliveryApproachingEvent 발행 완료: deliveryId={}, status={}", delivery.id, delivery.status)
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    fun handleArrived(event: RobotArrivedEvent) {
        log.info("RobotArrivedEvent 수신: robotId={}, destination={}", event.robotId, event.destination)

        val delivery = findDeliveryByRobotId(event.robotId) ?: return

        delivery.arrived()
        deliveryRepository.save(delivery)

        log.info("배달 도착 처리 완료: deliveryId={}, status={}", delivery.id, delivery.status)
    }

    private fun findDeliveryByRobotId(robotId: RobotId): Delivery? =
        robotRepository
            .findById(robotId)
            ?.currentDeliveryId
            ?.let { deliveryRepository.findById(it) }
}
