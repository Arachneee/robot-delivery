package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.BaseEntity
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent
import com.robotdelivery.domain.robot.event.RobotDestinationChangedEvent
import com.robotdelivery.infrastructure.persistence.converter.DeliveryIdConverter
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

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
    var status: RobotStatus = RobotStatus.READY,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var drivingStatus: RobotDrivingStatus = RobotDrivingStatus.ARRIVED,
    @Column(name = "current_delivery_id", nullable = true)
    @Convert(converter = DeliveryIdConverter::class)
    var currentDeliveryId: DeliveryId? = null,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "latitude", column = Column(name = "destination_latitude", nullable = true)),
        AttributeOverride(name = "longitude", column = Column(name = "destination_longitude", nullable = true)),
    )
    var destination: Location? = null,
) : BaseEntity<Robot>() {
    fun startDuty() {
        check(status.canTransitionTo(RobotStatus.READY)) {
            "출근할 수 없는 상태입니다. 현재 상태: $status"
        }
        transitionTo(RobotStatus.READY)

        registerEvent(RobotBecameAvailableEvent(robotId = getRobotId()))
    }

    fun endDuty() {
        check(status.canTransitionTo(RobotStatus.OFF_DUTY)) {
            "퇴근할 수 없는 상태입니다. 현재 상태: $status"
        }
        check(currentDeliveryId == null) {
            "배달 수행 중에는 퇴근할 수 없습니다."
        }
        transitionTo(RobotStatus.OFF_DUTY)
    }

    fun assignDelivery(
        deliveryId: DeliveryId,
        pickupLocation: Location,
    ) {
        check(status.isAvailableForDelivery()) {
            "배달을 받을 수 없는 상태입니다. 현재 상태: $status"
        }
        check(currentDeliveryId == null) {
            "이미 다른 배달을 수행 중입니다."
        }
        this.currentDeliveryId = deliveryId
        transitionTo(RobotStatus.BUSY)
        navigateTo(pickupLocation)
    }

    fun completeDelivery() {
        check(status == RobotStatus.BUSY) {
            "배달 수행 중이 아닙니다. 현재 상태: $status"
        }
        check(currentDeliveryId != null) {
            "할당된 배달이 없습니다."
        }
        this.currentDeliveryId = null
        this.destination = null
        this.drivingStatus = RobotDrivingStatus.ARRIVED
        transitionTo(RobotStatus.READY)

        registerEvent(RobotBecameAvailableEvent(robotId = getRobotId()))
    }

    fun unassignDelivery() {
        check(status == RobotStatus.BUSY) {
            "배달 수행 중이 아닙니다. 현재 상태: $status"
        }
        check(currentDeliveryId != null) {
            "할당된 배달이 없습니다."
        }

        this.currentDeliveryId = null
        this.destination = null
        this.drivingStatus = RobotDrivingStatus.ARRIVED
        transitionTo(RobotStatus.READY)

        registerEvent(RobotBecameAvailableEvent(robotId = getRobotId()))
    }

    fun navigateTo(newDestination: Location) {
        check(status == RobotStatus.BUSY) {
            "배달 수행 중이 아닙니다. 현재 상태: $status"
        }
        this.destination = newDestination
        this.drivingStatus = RobotDrivingStatus.ON_GOING

        registerEvent(RobotDestinationChangedEvent(robotId = getRobotId(), destination = newDestination))
    }

    fun updateDrivingStatus(newLocation: Location) {
        val dest = destination ?: return

        val distanceTo = newLocation.distanceTo(dest)
        while (drivingStatus.isAbleToNextStatus(distanceTo)) {
            drivingStatus = drivingStatus.nextStatus!!
            drivingStatus.createEvent(robotId = getRobotId(), destination = dest)?.let { registerEvent(it) }
        }
    }

    fun isAvailableForDelivery(): Boolean = status.isAvailableForDelivery() && currentDeliveryId == null

    fun getRobotId(): RobotId = RobotId(id)

    private fun transitionTo(newStatus: RobotStatus) {
        check(status.canTransitionTo(newStatus)) {
            "잘못된 상태 전이입니다: $status -> $newStatus"
        }
        this.status = newStatus
    }

    override fun toString(): String =
        "Robot(id=$id, name='$name', status=$status, drivingStatus=$drivingStatus, currentDeliveryId=$currentDeliveryId)"
}
