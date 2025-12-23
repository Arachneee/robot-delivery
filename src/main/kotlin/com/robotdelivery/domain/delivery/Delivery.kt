@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.BaseEntity
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.event.*
import jakarta.persistence.*

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
) : BaseEntity<Delivery>() {
    @Transient
    private var isNew: Boolean = (id == 0L)

    @PostPersist
    fun onPostPersist() {
        if (isNew) {
            registerEvent(
                DeliveryCreatedEvent(
                    deliveryId = getDeliveryId(),
                    pickupLocation = pickupDestination.location,
                    deliveryLocation = deliveryDestination.location,
                ),
            )
            isNew = false
        }
    }

    fun assignRobot(robotId: RobotId) {
        check(status == DeliveryStatus.PENDING) {
            "대기 상태의 배달만 로봇 배차가 가능합니다. 현재 상태: $status"
        }
        this.assignedRobotId = robotId
        transitionTo(DeliveryStatus.ASSIGNED)

        registerEvent(
            DeliveryRobotAssignedEvent(
                deliveryId = getDeliveryId(),
                robotId = robotId,
                pickupLocation = pickupDestination.location,
            ),
        )
    }

    fun unassignRobot() {
        check(status.isUnassignable()) {
            "배달 출발 전 상태에서만 배차 취소가 가능합니다. 현재 상태: $status"
        }
        val robotId =
            this.assignedRobotId
                ?: throw IllegalStateException("배차된 로봇이 없습니다.")

        this.assignedRobotId = null
        transitionTo(DeliveryStatus.PENDING)

        registerEvent(
            DeliveryRobotUnassignedEvent(
                deliveryId = getDeliveryId(),
                robotId = robotId,
            ),
        )
    }

    fun reassignRobot(newRobotId: RobotId): Location {
        check(status.isReassignable()) {
            "배차 변경이 불가능한 상태입니다. 현재 상태: $status"
        }
        val previousRobotId =
            this.assignedRobotId
                ?: throw IllegalStateException("배차된 로봇이 없습니다.")

        check(previousRobotId != newRobotId) {
            "동일한 로봇으로는 배차 변경할 수 없습니다."
        }

        val newRobotDestination = getCurrentDestination()!!.location

        this.assignedRobotId = newRobotId

        registerEvent(
            DeliveryRobotReassignedEvent(
                deliveryId = getDeliveryId(),
                previousRobotId = previousRobotId,
                newRobotId = newRobotId,
                newRobotDestination = newRobotDestination,
            ),
        )

        return newRobotDestination
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

    fun startDelivery() {
        check(status == DeliveryStatus.PICKING_UP) {
            "픽업 중 상태에서만 배송을 시작할 수 있습니다. 현재 상태: $status"
        }
        transitionTo(DeliveryStatus.DELIVERING)

        registerEvent(
            DeliveryStartedEvent(
                deliveryId = getDeliveryId(),
                robotId = assignedRobotId!!,
            ),
        )
    }

    fun complete() {
        check(status == DeliveryStatus.DROPPING_OFF) {
            "배달 완료 처리할 수 없는 상태입니다. 현재 상태: $status"
        }
        transitionTo(DeliveryStatus.COMPLETED)

        registerEvent(
            DeliveryCompletedEvent(
                deliveryId = getDeliveryId(),
                robotId = assignedRobotId!!,
            ),
        )
    }

    fun completeReturn() {
        check(status == DeliveryStatus.RETURNING_OFF) {
            "회수 완료 처리할 수 없는 상태입니다. 현재 상태: $status"
        }
        transitionTo(DeliveryStatus.RETURN_COMPLETED)

        registerEvent(
            DeliveryReturnCompletedEvent(
                deliveryId = getDeliveryId(),
                robotId = assignedRobotId!!,
            ),
        )
    }

    fun cancel() {
        val requiresReturn = status.requiresReturn()
        val nextStatus =
            when {
                status.isCancelable() -> DeliveryStatus.CANCELED
                requiresReturn -> DeliveryStatus.RETURNING
                else -> throw IllegalStateException("취소할 수 없는 상태입니다. 현재 상태: $status")
            }
        transitionTo(nextStatus)

        if (requiresReturn) {
            registerEvent(
                DeliveryReturnStartedEvent(
                    deliveryId = getDeliveryId(),
                    robotId = assignedRobotId!!,
                    returnLocation = pickupDestination.location,
                ),
            )
        }

        registerEvent(
            DeliveryCanceledEvent(
                deliveryId = getDeliveryId(),
                robotId = assignedRobotId,
                requiresReturn = requiresReturn,
            ),
        )
    }

    private fun transitionTo(newStatus: DeliveryStatus) {
        check(status.canTransitionTo(newStatus)) {
            "잘못된 상태 전이입니다: $status -> $newStatus"
        }
        this.status = newStatus
    }

    fun isActive(): Boolean =
        status !in
            listOf(
                DeliveryStatus.COMPLETED,
                DeliveryStatus.CANCELED,
                DeliveryStatus.RETURN_COMPLETED,
            )

    fun getCurrentDestination(): Destination? =
        when (status) {
            DeliveryStatus.PENDING -> null
            DeliveryStatus.ASSIGNED, DeliveryStatus.PICKUP_ARRIVED, DeliveryStatus.PICKING_UP -> pickupDestination
            DeliveryStatus.DELIVERING, DeliveryStatus.DELIVERY_ARRIVED, DeliveryStatus.DROPPING_OFF -> deliveryDestination
            DeliveryStatus.RETURNING, DeliveryStatus.RETURN_ARRIVED, DeliveryStatus.RETURNING_OFF -> pickupDestination
            DeliveryStatus.COMPLETED, DeliveryStatus.CANCELED, DeliveryStatus.RETURN_COMPLETED -> null
        }

    fun getDeliveryId(): DeliveryId = DeliveryId(id)

    override fun toString(): String =
        "Delivery(id=$id, phoneNumber='$phoneNumber', status=$status, assignedRobotId=$assignedRobotId, isNew=$isNew)"
}
