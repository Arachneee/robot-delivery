package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.vo.RobotId

interface RobotIotStateRepository {
    fun save(state: RobotIotState)

    fun findById(robotId: RobotId): RobotIotState?

    fun findAll(): List<RobotIotState>

    fun deleteById(robotId: RobotId)
}
