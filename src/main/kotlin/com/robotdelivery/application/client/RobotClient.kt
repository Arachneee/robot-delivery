package com.robotdelivery.application.client

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId

interface RobotClient {
    fun navigateTo(
        robotId: RobotId,
        destination: Location,
    )

    fun openLid(robotId: RobotId)
}
