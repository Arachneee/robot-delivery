package com.robotdelivery.application.command

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.application.command.vo.ChangeStatusResult
import com.robotdelivery.application.command.vo.CreateDeliveryCommand
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.OrderNo
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.getById
import com.robotdelivery.domain.order.Order
import com.robotdelivery.domain.order.OrderRepository
import com.robotdelivery.domain.order.getByOrderNo
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.getById
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
    private val orderRepository: OrderRepository,
    private val robotRepository: RobotRepository,
    private val robotClient: RobotClient,
) {
    fun createDelivery(command: CreateDeliveryCommand): DeliveryId {
        val order = command.toOrder()
        val savedOrder = orderRepository.saveAndFlush(order)

        val totalVolume = savedOrder.calculateTotalVolume()
        val delivery = command.toDelivery(savedOrder.getOrderId(), totalVolume)

        return deliveryRepository.saveAndFlush(delivery).getDeliveryId()
    }

    fun createAdditionalDelivery(orderNo: OrderNo): DeliveryId {
        val order = orderRepository.getByOrderNo(orderNo)
        return createDeliveryFromOrder(order)
    }

    private fun createDeliveryFromOrder(order: Order): DeliveryId {
        val totalVolume = order.calculateTotalVolume()
        val delivery = Delivery(
            orderId = order.getOrderId(),
            pickupDestination = order.pickupDestination,
            deliveryDestination = order.deliveryDestination,
            phoneNumber = order.phoneNumber,
            totalVolume = totalVolume,
        )

        return deliveryRepository.saveAndFlush(delivery).getDeliveryId()
    }

    fun startDelivery(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        val robot = robotRepository.getById(delivery.getAssignedRobotIdOrThrow())

        delivery.startDelivery()
        robot.navigateTo(delivery.getCurrentDestination().location)

        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }

    fun completeDelivery(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        val robot = robotRepository.getById(delivery.getAssignedRobotIdOrThrow())

        delivery.complete()
        robot.completeDelivery()

        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }

    fun completeReturn(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        val robot = robotRepository.getById(delivery.getAssignedRobotIdOrThrow())

        delivery.completeReturn()
        robot.completeDelivery()

        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }

    fun cancelDelivery(deliveryId: DeliveryId): Boolean {
        val delivery = deliveryRepository.getById(deliveryId)
        val requiresReturn = delivery.status.requiresReturn()

        delivery.cancel()

        delivery.assignedRobotId?.let { robotId ->
            robotRepository
                .getById(robotId)
                .apply {
                    if (requiresReturn) navigateTo(delivery.getCurrentDestination().location) else completeDelivery()
                }.also { robotRepository.save(it) }
        }

        deliveryRepository.save(delivery)
        return requiresReturn
    }

    fun unassignRobot(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        val robot = robotRepository.getById(delivery.getAssignedRobotIdOrThrow())

        delivery.unassignRobot()
        robot.unassignDelivery()

        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }

    fun reassignRobot(
        deliveryId: DeliveryId,
        newRobotId: RobotId,
    ): RobotId? {
        val delivery = deliveryRepository.getById(deliveryId)
        val previousRobotId = delivery.assignedRobotId

        val newRobot =
            robotRepository.getById(newRobotId).also {
                check(it.isAvailableForDelivery()) { "새 로봇이 배차 가능한 상태가 아닙니다." }
            }

        previousRobotId?.let { prevId ->
            delivery.reassignRobot(newRobotId)
            robotRepository.getById(prevId).apply { unassignDelivery() }.also { robotRepository.save(it) }
        } ?: delivery.assignRobot(newRobotId)

        newRobot.assignDelivery(deliveryId, delivery.getCurrentDestination().location)

        deliveryRepository.save(delivery)
        robotRepository.save(newRobot)

        return previousRobotId
    }

    fun openDoor(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        val robotId = delivery.getAssignedRobotIdOrThrow()

        delivery.openDoor()
        deliveryRepository.save(delivery)

        robotClient.openDoor(robotId)
    }

    fun changeStatus(
        deliveryId: DeliveryId,
        targetStatus: DeliveryStatus,
    ): ChangeStatusResult {
        val delivery = deliveryRepository.getById(deliveryId)
        val previousStatus = delivery.status

        check(previousStatus.canTransitionTo(targetStatus)) {
            "현재 상태($previousStatus)에서 $targetStatus 상태로 변경할 수 없습니다."
        }

        check(targetStatus != DeliveryStatus.ASSIGNED) {
            "ASSIGNED 상태로의 변경은 배차 API를 사용해주세요."
        }

        when (targetStatus) {
            DeliveryStatus.PENDING -> unassignRobot(deliveryId)

            DeliveryStatus.ASSIGNED -> error("unreachable")

            DeliveryStatus.PICKUP_ARRIVED,
            DeliveryStatus.DELIVERY_ARRIVED,
            DeliveryStatus.RETURN_ARRIVED,
            -> arrive(deliveryId)

            DeliveryStatus.PICKING_UP,
            DeliveryStatus.DROPPING_OFF,
            DeliveryStatus.RETURNING_OFF,
            -> openDoor(deliveryId)

            DeliveryStatus.DELIVERING -> startDelivery(deliveryId)

            DeliveryStatus.COMPLETED -> completeDelivery(deliveryId)

            DeliveryStatus.CANCELED, DeliveryStatus.RETURNING -> cancelDelivery(deliveryId)

            DeliveryStatus.RETURN_COMPLETED -> completeReturn(deliveryId)
        }

        return ChangeStatusResult(
            previousStatus = previousStatus,
            currentStatus = deliveryRepository.getById(deliveryId).status,
        )
    }

    private fun arrive(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        delivery.arrived()
        deliveryRepository.save(delivery)
    }
}
