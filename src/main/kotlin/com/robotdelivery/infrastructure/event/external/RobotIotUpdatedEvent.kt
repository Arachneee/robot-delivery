package com.robotdelivery.infrastructure.event.external

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId

data class RobotIotUpdatedEvent(
    val robotId: RobotId,
    val location: Location,
    val battery: Int,
    val doorOpen: Boolean,
    val loadWeight: Double,
)
