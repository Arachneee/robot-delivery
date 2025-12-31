package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId

interface RobotClient {
    fun navigateTo(
        robotId: RobotId,
        destination: Location,
    )

    fun openDoor(robotId: RobotId)
}
