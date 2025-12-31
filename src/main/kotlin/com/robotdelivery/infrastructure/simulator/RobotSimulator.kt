package com.robotdelivery.infrastructure.simulator

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.robot.RobotClient
import com.robotdelivery.infrastructure.event.external.RobotIotUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.random.Random

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

        val distances = listOf(1000.0, 500.0, 40.0, 10.0, 1.0)
        val batteryStart = 100

        distances.forEachIndexed { index, distance ->
            scheduler.schedule(
                {
                    val currentLocation = calculateLocationAtDistance(destination, distance)
                    val currentBattery = batteryStart - (index + 1)
                    log.info(
                        "로봇 이동 중 시뮬레이션 (${index + 1}/5): robotId={}, distance={}m, location={}",
                        robotId,
                        distance,
                        currentLocation,
                    )
                    eventPublisher.publishEvent(
                        RobotIotUpdatedEvent(
                            robotId = robotId,
                            location = currentLocation,
                            battery = currentBattery,
                            doorOpen = false,
                            loadWeight = 0.0,
                        ),
                    )
                },
                (index + 1).toLong(),
                TimeUnit.SECONDS,
            )
        }
    }

    private fun calculateLocationAtDistance(
        origin: Location,
        distanceMeters: Double,
    ): Location {
        val earthRadius = 6371000.0
        val randomAngle = Random.nextDouble(0.0, 2 * Math.PI)

        val deltaLat = (distanceMeters / earthRadius) * cos(randomAngle)
        val deltaLon =
            (distanceMeters / earthRadius) * kotlin.math.sin(randomAngle) / cos(Math.toRadians(origin.latitude))

        return Location(
            latitude = origin.latitude + Math.toDegrees(deltaLat),
            longitude = origin.longitude + Math.toDegrees(deltaLon),
        )
    }

    override fun openDoor(robotId: RobotId) {
        log.info("로봇 뚜껑 열기 시뮬레이션: robotId={}", robotId)
        // 뚜껑 열기는 즉시 완료로 처리
    }
}
