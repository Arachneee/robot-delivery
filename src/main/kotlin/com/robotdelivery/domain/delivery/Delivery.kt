package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.DeliveryId
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
@Table(name = "deliveries")
class Delivery(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    val id: Long = 0L,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "address", column = Column(name = "pickup_address")),
        AttributeOverride(name = "addressDetail", column = Column(name = "pickup_address_detail")),
        AttributeOverride(name = "location.latitude", column = Column(name = "pickup_latitude")),
        AttributeOverride(name = "location.longitude", column = Column(name = "pickup_longitude")),
    )
    val pickupDestination: Destination,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "address", column = Column(name = "delivery_address")),
        AttributeOverride(name = "addressDetail", column = Column(name = "delivery_address_detail")),
        AttributeOverride(name = "location.latitude", column = Column(name = "delivery_latitude")),
        AttributeOverride(name = "location.longitude", column = Column(name = "delivery_longitude")),
    )
    val deliveryDestination: Destination,
    @Column(nullable = false)
    val phoneNumber: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeliveryStatus = DeliveryStatus.PENDING,
    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "assigned_robot_id"))
    var assignedRobotId: RobotId? = null,
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Column
    var completedAt: LocalDateTime? = null,
) {
    fun getDeliveryId(): DeliveryId = DeliveryId(id)

    fun assignRobot(robotId: RobotId) {
        require(status == DeliveryStatus.PENDING) {
            "대기 상태의 배달만 로봇 배차가 가능합니다. 현재 상태: $status"
        }
        this.assignedRobotId = robotId
        transitionTo(DeliveryStatus.ASSIGNED)
    }

    fun startDelivery() {
        require(status == DeliveryStatus.PICKING_UP) {
            "픽업 중 상태에서만 배송을 시작할 수 있습니다. 현재 상태: $status"
        }
        transitionTo(DeliveryStatus.DELIVERING)
    }

    fun arrived() {
        val nextStatus =
            when (status) {
                DeliveryStatus.ASSIGNED -> DeliveryStatus.PICKUP_ARRIVED
                DeliveryStatus.DELIVERING -> DeliveryStatus.DELIVERY_ARRIVED
                DeliveryStatus.RETURNING -> DeliveryStatus.RETURN_ARRIVED
                else -> throw IllegalStateException("도착 처리할 수 없는 상태입니다. 현재 상태: $status")
            }
        transitionTo(nextStatus)
    }

    fun openDoor() {
        val nextStatus =
            when (status) {
                DeliveryStatus.PICKUP_ARRIVED -> DeliveryStatus.PICKING_UP
                DeliveryStatus.DELIVERY_ARRIVED -> DeliveryStatus.DROPPING_OFF
                DeliveryStatus.RETURN_ARRIVED -> DeliveryStatus.RETURNING_OFF
                else -> throw IllegalStateException("문을 열 수 없는 상태입니다. 현재 상태: $status")
            }
        transitionTo(nextStatus)
    }

    fun complete() {
        require(status == DeliveryStatus.DROPPING_OFF) {
            "배달 완료 처리할 수 없는 상태입니다. 현재 상태: $status"
        }
        transitionTo(DeliveryStatus.COMPLETED)
        this.completedAt = LocalDateTime.now()
    }

    fun completeReturn() {
        require(status == DeliveryStatus.RETURNING_OFF) {
            "회수 완료 처리할 수 없는 상태입니다. 현재 상태: $status"
        }
        transitionTo(DeliveryStatus.RETURN_COMPLETED)
        this.completedAt = LocalDateTime.now()
    }

    fun cancel() {
        val nextStatus =
            when {
                status.isCancelable() -> DeliveryStatus.CANCELED
                status.requiresReturn() -> DeliveryStatus.RETURNING
                else -> throw IllegalStateException("취소할 수 없는 상태입니다. 현재 상태: $status")
            }
        transitionTo(nextStatus)
        if (nextStatus == DeliveryStatus.CANCELED) {
            this.completedAt = LocalDateTime.now()
        }
    }

    private fun transitionTo(newStatus: DeliveryStatus) {
        require(status.canTransitionTo(newStatus)) {
            "잘못된 상태 전이입니다: $status -> $newStatus"
        }
        this.status = newStatus
        this.updatedAt = LocalDateTime.now()
    }

    fun getCurrentDestination(): Destination? =
        when (status) {
            DeliveryStatus.PENDING -> null
            DeliveryStatus.ASSIGNED, DeliveryStatus.PICKUP_ARRIVED, DeliveryStatus.PICKING_UP -> pickupDestination
            DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERY_ARRIVED, DeliveryStatus.DROPPING_OFF -> deliveryDestination
            DeliveryStatus.RETURNING, DeliveryStatus.RETURN_ARRIVED, DeliveryStatus.RETURNING_OFF -> pickupDestination
            DeliveryStatus.COMPLETED, DeliveryStatus.CANCELED, DeliveryStatus.RETURN_COMPLETED -> null
        }

    fun isActive(): Boolean =
        status !in
            listOf(
                DeliveryStatus.COMPLETED,
                DeliveryStatus.CANCELED,
                DeliveryStatus.RETURN_COMPLETED,
            )
}
