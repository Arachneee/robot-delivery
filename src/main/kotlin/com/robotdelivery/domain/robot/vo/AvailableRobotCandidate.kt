package com.robotdelivery.domain.robot.vo

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotIotState

data class AvailableRobotCandidate(
    val robot: Robot,
    val iotState: RobotIotState,
) {
    val currentLocation: Location
        get() = iotState.location
}
