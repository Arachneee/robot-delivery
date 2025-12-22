package com.robotdelivery.domain.robot.event

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import java.time.LocalDateTime
import java.util.UUID

sealed class RobotEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

class RobotBecameAvailableEvent(
    val robotId: RobotId,
    val location: Location,
) : RobotEvent()

class RobotArrivedAtDestinationEvent(
    val robotId: RobotId,
    val destination: Location,
) : RobotEvent()

class RobotDestinationChangedEvent(
    val robotId: RobotId,
    val destination: Location,
) : RobotEvent()
