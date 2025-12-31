package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import java.time.Instant

data class RobotIotState(
    val robotId: RobotId,
    val location: Location,
    val battery: Int,
    val doorOpen: Boolean,
    val loadWeight: Double,
    val updatedAt: Instant = Instant.now(),
) {
    init {
        require(battery in 0..100) {
            "배터리는 0에서 100 사이여야 합니다."
        }
        require(loadWeight >= 0) {
            "적재 무게는 0 이상이어야 합니다."
        }
    }

    fun hasSufficientBattery(minimumBattery: Int = 20): Boolean = battery >= minimumBattery

    fun distanceTo(destination: Location): Double = location.distanceTo(destination)
}
