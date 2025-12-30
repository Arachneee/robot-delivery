package com.robotdelivery.application.eventhandler

import com.robotdelivery.application.command.RobotService
import com.robotdelivery.domain.robot.RobotIotState
import com.robotdelivery.infrastructure.event.external.RobotIotUpdatedEvent
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class RobotIotEventHandler(
    private val robotService: RobotService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    @Transactional
    fun handle(event: RobotIotUpdatedEvent) {
        log.debug(
            "RobotIotUpdatedEvent 수신: robotId={}, location={}, battery={}, doorOpen={}, loadWeight={}",
            event.robotId,
            event.location,
            event.battery,
            event.doorOpen,
            event.loadWeight,
        )

        val iotState =
            RobotIotState(
                robotId = event.robotId,
                location = event.location,
                battery = event.battery,
                doorOpen = event.doorOpen,
                loadWeight = event.loadWeight,
            )

        robotService.updateIotState(iotState)
    }
}
