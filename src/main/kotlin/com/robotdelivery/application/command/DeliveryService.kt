package com.robotdelivery.application.command

import com.robotdelivery.application.command.vo.ChangeStatusResult
import com.robotdelivery.application.command.vo.CreateDeliveryCommand
import com.robotdelivery.application.command.vo.CreateDeliveryResult
import com.robotdelivery.domain.common.vo.DeliveryId
import com.robotdelivery.domain.common.vo.OrderNo
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.delivery.DeliveryAssignmentService
import com.robotdelivery.domain.delivery.DeliveryFactory
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.getById
import com.robotdelivery.domain.delivery.vo.AssignmentResult
import com.robotdelivery.domain.delivery.vo.DeliveryCreationResult
import com.robotdelivery.domain.delivery.vo.DeliveryStatus
import com.robotdelivery.domain.order.Order
import com.robotdelivery.domain.order.OrderRepository
import com.robotdelivery.domain.order.getByOrderNo
import com.robotdelivery.domain.robot.RobotClient
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
    private val deliveryAssignmentService: DeliveryAssignmentService,
    private val deliveryFactory: DeliveryFactory,
) {
    fun createDelivery(command: CreateDeliveryCommand): CreateDeliveryResult {
        val order = command.toOrder()
        val savedOrder = orderRepository.saveAndFlush(order)

        val totalVolume = savedOrder.calculateTotalVolume()

        val result =
            deliveryFactory.create(
                pickupDestination = command.pickupDestination.toDestination(),
                deliveryDestination = command.deliveryDestination.toDestination(),
                phoneNumber = command.phoneNumber,
                orderId = savedOrder.getOrderId(),
                totalVolume = totalVolume,
            )

        return saveDelivery(result)
    }

    fun createAdditionalDelivery(orderNo: OrderNo): CreateDeliveryResult {
        val order = orderRepository.getByOrderNo(orderNo)
        return createDeliveryFromOrder(order)
    }

    private fun createDeliveryFromOrder(order: Order): CreateDeliveryResult {
        val totalVolume = order.calculateTotalVolume()

        val result =
            deliveryFactory.create(
                pickupDestination = order.pickupDestination,
                deliveryDestination = order.deliveryDestination,
                phoneNumber = order.phoneNumber,
                orderId = order.getOrderId(),
                totalVolume = totalVolume,
            )

        return saveDelivery(result)
    }

    private fun saveDelivery(result: DeliveryCreationResult): CreateDeliveryResult =
        when (result) {
            is DeliveryCreationResult.RouteNotAvailable -> {
                throw IllegalStateException("배달 경로를 찾을 수 없습니다.")
            }

            is DeliveryCreationResult.Success -> {
                val savedDelivery = deliveryRepository.saveAndFlush(result.delivery)
                CreateDeliveryResult(
                    deliveryId = savedDelivery.getDeliveryId(),
                    estimatedPickupDuration = result.routeResult.toPickupDuration,
                    estimatedDeliveryDuration = result.routeResult.toDeliveryDuration,
                )
            }
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
        val previousRobot = previousRobotId?.let { robotRepository.getById(it) }
        val newRobot = robotRepository.getById(newRobotId)

        val result = deliveryAssignmentService.assignSpecificRobotToDelivery(delivery, newRobot, previousRobot)
        if (result is AssignmentResult.Failure) {
            throw IllegalStateException(result.message)
        }

        deliveryRepository.save(delivery)
        robotRepository.save(newRobot)
        previousRobot?.let { robotRepository.save(it) }

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
