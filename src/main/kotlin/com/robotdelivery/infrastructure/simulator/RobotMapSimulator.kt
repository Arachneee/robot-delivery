package com.robotdelivery.infrastructure.simulator

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.robot.RobotMapClient
import com.robotdelivery.domain.robot.vo.RouteResult
import com.robotdelivery.domain.robot.vo.RouteSegment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.math.roundToLong

@Component
class RobotMapSimulator : RobotMapClient {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun findRoute(waypoints: List<Location>): RouteResult {
        log.info("경로 조회 시뮬레이션: waypoints={}", waypoints)

        if (waypoints.size < 2) return RouteResult.unavailable()

        val segments =
            waypoints.zipWithNext().map { (from, to) ->
                val distanceMeters = from.distanceTo(to)
                val durationSeconds = (distanceMeters / ROBOT_SPEED_METERS_PER_SECOND).roundToLong()
                RouteSegment(isAvailable = true, duration = Duration.ofSeconds(durationSeconds))
            }

        return RouteResult(segments = segments)
    }

    companion object {
        private const val ROBOT_SPEED_METERS_PER_SECOND = 1.5
    }
}
