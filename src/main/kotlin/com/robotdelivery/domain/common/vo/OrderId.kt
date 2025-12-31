package com.robotdelivery.domain.common.vo

@JvmInline
value class OrderId(
    val value: Long,
) {
    init {
        require(value > 0) { "OrderId는 0보다 커야 합니다: $value" }
    }

    override fun toString(): String = value.toString()
}
