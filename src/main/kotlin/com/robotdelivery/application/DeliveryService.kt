package com.robotdelivery.application

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.application.command.ChangeStatusResult
import com.robotdelivery.application.command.CreateDeliveryCommand
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.getById
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.getById
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val robotClient: RobotClient,
) {
    fun createDelivery(command: CreateDeliveryCommand): DeliveryId {
        val delivery = command.toDelivery()
        val savedDelivery = deliveryRepository.saveAndFlush(delivery)
        return savedDelivery.getDeliveryId()
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

        val robotId = delivery.assignedRobotId
        if (robotId != null) {
            val robot = robotRepository.getById(robotId)

            if (requiresReturn) {
                robot.navigateTo(delivery.getCurrentDestination().location)
            } else {
                robot.completeDelivery()
            }
            robotRepository.save(robot)
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

        val newRobot = robotRepository.getById(newRobotId)
        check(newRobot.isAvailable()) {
            "새 로봇이 배차 가능한 상태가 아닙니다."
        }

        if (previousRobotId == null) {
            delivery.assignRobot(newRobotId)
        } else {
            delivery.reassignRobot(newRobotId)

            val previousRobot = robotRepository.getById(previousRobotId)
            previousRobot.unassignDelivery()

            robotRepository.save(previousRobot)
        }

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
