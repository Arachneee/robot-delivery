package com.robotdelivery.domain.common.vo

@JvmInline
value class DeliveryId(
    val value: Long,
) {
    init {
        require(value > 0) { "DeliveryId는 0보다 커야 합니다: $value" }
    }

    override fun toString(): String = value.toString()
}
