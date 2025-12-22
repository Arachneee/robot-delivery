package com.robotdelivery.application.eventhandler

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.domain.robot.event.RobotDeliveryAssignedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class RobotNavigationEventHandler(
    private val robotClient: RobotClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    fun handle(event: RobotDeliveryAssignedEvent) {
        log.info(
            "RobotDeliveryAssignedEvent 수신: robotId={}, deliveryId={}, pickupLocation={}",
            event.robotId,
            event.deliveryId,
            event.pickupLocation,
        )

        robotClient.navigateTo(event.robotId, event.pickupLocation)

        log.info(
            "로봇 이동 명령 전송 완료: robotId={}, destination={}",
            event.robotId,
            event.pickupLocation,
        )
    }
}
