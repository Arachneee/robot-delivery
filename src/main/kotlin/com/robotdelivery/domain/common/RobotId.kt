package com.robotdelivery.domain.common

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class RobotId(
    @Column(name = "robot_id", nullable = false)
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
