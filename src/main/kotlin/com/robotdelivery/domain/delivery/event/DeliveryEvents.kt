package com.robotdelivery.domain.delivery.event

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import java.time.LocalDateTime
import java.util.UUID

sealed class DeliveryEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId? = null,
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

class DeliveryCreatedEvent(
    deliveryId: DeliveryId,
    val pickupLocation: Location,
    val deliveryLocation: Location,
) : DeliveryEvent(deliveryId = deliveryId, robotId = null)

class DeliveryRobotAssignedEvent(
    deliveryId: DeliveryId,
    robotId: RobotId,
    val pickupLocation: Location,
) : DeliveryEvent(deliveryId = deliveryId, robotId = robotId)

class DeliveryStartedEvent(
    deliveryId: DeliveryId,
    robotId: RobotId,
) : DeliveryEvent(deliveryId = deliveryId, robotId = robotId)

class DeliveryCompletedEvent(
    deliveryId: DeliveryId,
    robotId: RobotId,
) : DeliveryEvent(deliveryId = deliveryId, robotId = robotId)

class DeliveryCanceledEvent(
    deliveryId: DeliveryId,
    robotId: RobotId?,
    val requiresReturn: Boolean,
) : DeliveryEvent(deliveryId = deliveryId, robotId = robotId)

class DeliveryReturnStartedEvent(
    deliveryId: DeliveryId,
    robotId: RobotId,
    val returnLocation: Location,
) : DeliveryEvent(deliveryId = deliveryId, robotId = robotId)

class DeliveryReturnCompletedEvent(
    deliveryId: DeliveryId,
    robotId: RobotId,
) : DeliveryEvent(deliveryId = deliveryId, robotId = robotId)

class DeliveryRobotUnassignedEvent(
    deliveryId: DeliveryId,
    robotId: RobotId,
) : DeliveryEvent(deliveryId = deliveryId, robotId = robotId)

class DeliveryRobotReassignedEvent(
    deliveryId: DeliveryId,
    val previousRobotId: RobotId,
    val newRobotId: RobotId,
    val newRobotDestination: Location,
) : DeliveryEvent(deliveryId = deliveryId, robotId = newRobotId)
