package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.infrastructure.event.external.RobotLocationUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class RobotLocationEventHandler(
    private val robotRepository: RobotRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun handle(event: RobotLocationUpdatedEvent) {
        log.info("RobotLocationUpdatedEvent 수신: robotId={}, location={}", event.robotId, event.location)

        val robot =
            robotRepository.findById(event.robotId) ?: run {
                log.warn("로봇을 찾을 수 없음: robotId={}", event.robotId)
                return
            }

        robot.updateLocation(event.location)
        robotRepository.save(robot)
    }
}
