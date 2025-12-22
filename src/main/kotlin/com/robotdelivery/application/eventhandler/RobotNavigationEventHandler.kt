package com.robotdelivery.application.eventhandler

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.domain.robot.event.RobotDestinationChangedEvent
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
    fun handle(event: RobotDestinationChangedEvent) {
        log.info(
            "RobotDestinationChangedEvent 수신: robotId={}, destination={}",
            event.robotId,
            event.destination,
        )

        robotClient.navigateTo(event.robotId, event.destination)

        log.info(
            "로봇 이동 명령 전송 완료: robotId={}, destination={}",
            event.robotId,
            event.destination,
        )
    }
}
