package com.robotdelivery.infrastructure.event.external

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId

data class RobotLocationUpdatedEvent(
    val robotId: RobotId,
    val location: Location,
)
