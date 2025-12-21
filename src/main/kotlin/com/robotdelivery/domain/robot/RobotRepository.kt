package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.RobotId

interface RobotRepository {
    fun findById(id: RobotId): Robot?

    fun findAll(): List<Robot>

    fun findAvailableRobots(): List<Robot>

    fun save(robot: Robot): Robot
}
