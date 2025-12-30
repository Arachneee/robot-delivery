package com.robotdelivery.domain.common

@JvmInline
value class RobotId(val value: Long) {
    init {
        require(value > 0) { "RobotId는 0보다 커야 합니다: $value" }
    }

    override fun toString(): String = value.toString()
}
