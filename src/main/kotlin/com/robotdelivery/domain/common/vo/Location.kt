package com.robotdelivery.domain.common.vo

import jakarta.persistence.Embeddable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Embeddable
data class Location(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "위도는 -90에서 90 사이여야 합니다." }
        require(longitude in -180.0..180.0) { "경도는 -180에서 180 사이여야 합니다." }
    }

    fun distanceTo(other: Location): Double {
        val earthRadius = 6371000.0

        val lat1Rad = Math.toRadians(this.latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLat = Math.toRadians(other.latitude - this.latitude)
        val deltaLon = Math.toRadians(other.longitude - this.longitude)

        val a =
            sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2) * sin(deltaLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}
