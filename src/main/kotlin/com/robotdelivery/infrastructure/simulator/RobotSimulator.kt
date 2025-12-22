package com.robotdelivery.infrastructure.simulator

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.infrastructure.event.external.RobotLocationUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class RobotSimulator(
    private val eventPublisher: ApplicationEventPublisher,
) : RobotClient {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scheduler = Executors.newScheduledThreadPool(1)

    @Async
    override fun navigateTo(
        robotId: RobotId,
        destination: Location,
    ) {
        log.info("로봇 이동 시뮬레이션 시작: robotId={}, destination={}", robotId, destination)

        scheduler.schedule(
            {
                log.info("로봇 이동 완료 시뮬레이션: robotId={}, location={}", robotId, destination)
                eventPublisher.publishEvent(RobotLocationUpdatedEvent(robotId, destination))
            },
            3,
            TimeUnit.SECONDS,
        )
    }

    override fun openLid(robotId: RobotId) {
        log.info("로봇 뚜껑 열기 시뮬레이션: robotId={}", robotId)
        // 뚜껑 열기는 즉시 완료로 처리
    }
}
