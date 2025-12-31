package com.robotdelivery.domain.robot.vo

import java.time.Duration

data class RouteResult(
    val segments: List<RouteSegment>,
) {
    val isAvailable: Boolean
        get() = segments.isNotEmpty() && segments.all { it.isAvailable }

    val totalDuration: Duration
        get() = segments.fold(Duration.ZERO) { acc, segment -> acc.plus(segment.duration) }

    val toPickupDuration: Duration
        get() = segments.firstOrNull()?.duration ?: Duration.ZERO

    val toDeliveryDuration: Duration
        get() = segments.drop(1).fold(Duration.ZERO) { acc, segment -> acc.plus(segment.duration) }

    companion object {
        fun of(
            toPickupSeconds: Long,
            toDeliverySeconds: Long,
        ): RouteResult =
            RouteResult(
                segments =
                    listOf(
                        RouteSegment(isAvailable = true, duration = Duration.ofSeconds(toPickupSeconds)),
                        RouteSegment(isAvailable = true, duration = Duration.ofSeconds(toDeliverySeconds)),
                    ),
            )

        fun unavailable(): RouteResult = RouteResult(segments = emptyList())
    }
}

data class RouteSegment(
    val isAvailable: Boolean,
    val duration: Duration,
)
