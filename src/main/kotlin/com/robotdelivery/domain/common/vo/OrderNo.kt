package com.robotdelivery.domain.common.vo

@JvmInline
value class OrderNo(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "OrderNo는 비어있을 수 없습니다." }
    }

    override fun toString(): String = value
}
