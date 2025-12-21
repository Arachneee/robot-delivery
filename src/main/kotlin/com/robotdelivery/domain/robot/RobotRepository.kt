package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.RobotId
import org.springframework.data.jpa.repository.JpaRepository

interface RobotRepository : JpaRepository<Robot, Long> {
    fun findById(id: RobotId): Robot? = findById(id.value).orElse(null)

    fun findAllByStatus(status: RobotStatus): List<Robot>
}
