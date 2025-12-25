package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.RobotId
import org.springframework.data.jpa.repository.JpaRepository

interface RobotRepository : JpaRepository<Robot, Long> {
    fun findAllByStatus(status: RobotStatus): List<Robot>
}

fun RobotRepository.findById(id: RobotId): Robot? = findById(id.value).orElse(null)

fun RobotRepository.getById(id: RobotId): Robot = findById(id) ?: throw IllegalArgumentException("로봇을 찾을 수 없습니다: $id")
