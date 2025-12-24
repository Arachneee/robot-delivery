package com.robotdelivery.domain.common

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class RobotId(
    @Column(name = "robot_id", nullable = false)
    val value: Long,
) {
    init {
        require(value > 0) { "RobotId는 0보다 커야 합니다: $value" }
    }

    override fun toString(): String = value.toString()
}
