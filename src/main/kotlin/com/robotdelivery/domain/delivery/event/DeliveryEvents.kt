package com.robotdelivery.domain.delivery.event

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.DomainEvent
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId

class DeliveryCreatedEvent(
    val deliveryId: DeliveryId,
    val pickupLocation: Location,
    val deliveryLocation: Location,
) : DomainEvent()

class RobotAssignedToDeliveryEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
    val pickupLocation: Location,
) : DomainEvent()

class DeliveryPickupArrivedEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
) : DomainEvent()

class DeliveryStartedEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
) : DomainEvent()

class DeliveryCompletedEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId,
) : DomainEvent()

class DeliveryCanceledEvent(
    val deliveryId: DeliveryId,
    val robotId: RobotId?,
    val requiresReturn: Boolean,
) : DomainEvent()

