package com.robotdelivery.domain.common

@JvmInline
value class Volume(val value: Double) {
    init {
        require(value >= 0) { "Volume은 0 이상이어야 합니다: $value" }
    }

    operator fun plus(other: Volume): Volume = Volume(value + other.value)

    operator fun times(quantity: Int): Volume = Volume(value * quantity)

    operator fun compareTo(other: Volume): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()

    companion object {
        val ZERO = Volume(0.0)
    }
}

