package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.event.RobotArrivedAtDestinationEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DeliveryArrivalEventHandler(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    fun handle(event: RobotArrivedAtDestinationEvent) {
        log.info("RobotArrivedAtDestinationEvent 수신: robotId={}, destination={}", event.robotId, event.destination)

        val robot =
            robotRepository.findById(event.robotId) ?: run {
                log.warn("로봇을 찾을 수 없음: robotId={}", event.robotId)
                return
            }

        val deliveryId =
            robot.currentDeliveryId ?: run {
                log.warn("로봇에 할당된 배달이 없음: robotId={}", event.robotId)
                return
            }

        val delivery =
            deliveryRepository.findById(deliveryId) ?: run {
                log.warn("배달을 찾을 수 없음: deliveryId={}", deliveryId)
                return
            }

        delivery.arrived()
        deliveryRepository.save(delivery)

        log.info("배달 도착 처리 완료: deliveryId={}, status={}", deliveryId, delivery.status)
    }
}
