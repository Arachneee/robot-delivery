package com.robotdelivery.domain.delivery.event

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import java.time.LocalDateTime
import java.util.UUID

sealed class DeliveryEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

class DeliveryCreatedEvent(
    val deliveryId: DeliveryId,
    val pickupLocation: Location,
    val deliveryLocation: Location,
) : DeliveryEvent()

class DeliveryRobotAssignedEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
    val pickupLocation: Location,
) : DeliveryEvent()

class DeliveryStartedEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
) : DeliveryEvent()

class DeliveryCompletedEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
) : DeliveryEvent()

class DeliveryCanceledEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId?,
    val requiresReturn: Boolean,
) : DeliveryEvent()

class DeliveryReturnStartedEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
    val returnLocation: Location,
) : DeliveryEvent()

class DeliveryReturnCompletedEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
) : DeliveryEvent()
