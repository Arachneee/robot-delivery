package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "robots")
class Robot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Column(nullable = false, unique = true)
    val name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RobotStatus = RobotStatus.OFF_DUTY,
    @Column(nullable = false)
    var battery: Int = 100,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "latitude", column = Column(name = "location_latitude")),
        AttributeOverride(name = "longitude", column = Column(name = "location_longitude")),
    )
    var location: Location,
    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "current_delivery_id"))
    var currentDeliveryId: DeliveryId? = null,
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun getRobotId(): RobotId = RobotId(id)

    fun startDuty() {
        require(status.canTransitionTo(RobotStatus.READY)) {
            "출근할 수 없는 상태입니다. 현재 상태: $status"
        }
        transitionTo(RobotStatus.READY)
    }

    fun endDuty() {
        require(status.canTransitionTo(RobotStatus.OFF_DUTY)) {
            "퇴근할 수 없는 상태입니다. 현재 상태: $status"
        }
        require(currentDeliveryId == null) {
            "배달 수행 중에는 퇴근할 수 없습니다."
        }
        transitionTo(RobotStatus.OFF_DUTY)
    }

    fun assignDelivery(deliveryId: DeliveryId) {
        require(status.isAvailableForDelivery()) {
            "배달을 받을 수 없는 상태입니다. 현재 상태: $status"
        }
        require(currentDeliveryId == null) {
            "이미 다른 배달을 수행 중입니다."
        }
        this.currentDeliveryId = deliveryId
        transitionTo(RobotStatus.BUSY)
    }

    fun completeDelivery() {
        require(status == RobotStatus.BUSY) {
            "배달 수행 중이 아닙니다. 현재 상태: $status"
        }
        require(currentDeliveryId != null) {
            "할당된 배달이 없습니다."
        }
        this.currentDeliveryId = null
        transitionTo(RobotStatus.READY)
    }

    fun updateBattery(newBattery: Int) {
        require(newBattery in 0..100) {
            "배터리는 0에서 100 사이여야 합니다."
        }
        this.battery = newBattery
        this.updatedAt = LocalDateTime.now()
    }

    fun updateLocation(newLocation: Location) {
        this.location = newLocation
        this.updatedAt = LocalDateTime.now()
    }

    private fun transitionTo(newStatus: RobotStatus) {
        require(status.canTransitionTo(newStatus)) {
            "잘못된 상태 전이입니다: $status -> $newStatus"
        }
        this.status = newStatus
        this.updatedAt = LocalDateTime.now()
    }

    fun isAvailable(): Boolean = status.isAvailableForDelivery() && currentDeliveryId == null && battery >= 20
}
