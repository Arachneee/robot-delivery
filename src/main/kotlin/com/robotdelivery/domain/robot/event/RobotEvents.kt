package com.robotdelivery.domain.robot.event

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.DomainEvent
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId

class RobotStartedDutyEvent(
    val robotId: RobotId,
    val location: Location,
) : DomainEvent()

class RobotEndedDutyEvent(
    val robotId: RobotId,
) : DomainEvent()

class RobotBecameAvailableEvent(
    val robotId: RobotId,
    val location: Location,
) : DomainEvent()

class RobotDeliveryAssignedEvent(
    val robotId: RobotId,
    val deliveryId: DeliveryId,
) : DomainEvent()

class RobotLocationUpdatedEvent(
    val robotId: RobotId,
    val location: Location,
) : DomainEvent()

class RobotBrokenEvent(
    val robotId: RobotId,
) : DomainEvent()

