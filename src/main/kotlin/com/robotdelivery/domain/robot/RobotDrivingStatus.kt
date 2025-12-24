package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.event.RobotApproachingEvent
import com.robotdelivery.domain.robot.event.RobotArrivedEvent
import com.robotdelivery.domain.robot.event.RobotEvent

enum class RobotDrivingStatus(
    val thresholdMeter: Double,
    private val nextStatusProvider: () -> RobotDrivingStatus?,
    private val eventFactory: ((RobotId, Location) -> RobotEvent)?,
) {
    ON_GOING(1500.0, { APPROACHING }, null),
    APPROACHING(50.0, { ARRIVED }, { robotId, dest -> RobotApproachingEvent(robotId, dest) }),
    ARRIVED(5.0, { null }, { robotId, dest -> RobotArrivedEvent(robotId, dest) }),
    ;

    val nextStatus: RobotDrivingStatus? by lazy { nextStatusProvider() }

    fun isAbleToNextStatus(distanceTo: Double): Boolean = nextStatus?.let { distanceTo <= it.thresholdMeter } ?: false

    fun createEvent(
        robotId: RobotId,
        destination: Location,
    ): RobotEvent? = eventFactory?.invoke(robotId, destination)
}
