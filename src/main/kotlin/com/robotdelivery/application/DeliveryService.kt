package com.robotdelivery.application

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.application.command.CreateDeliveryCommand
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.DeliveryRepository
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

    fun completeDelivery(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        val robot = robotRepository.getById(delivery.getAssignedRobotIdOrThrow())

        delivery.complete()
        robot.completeDelivery()

        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }

    fun openDoor(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        val robotId = delivery.getAssignedRobotIdOrThrow()

        delivery.openDoor()
        deliveryRepository.save(delivery)

        robotClient.openDoor(robotId)
    }

    fun startDelivery(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)
        val robot = robotRepository.getById(delivery.getAssignedRobotIdOrThrow())

        delivery.startDelivery()
        robot.navigateTo(delivery.deliveryDestination.location)

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
                robot.navigateTo(delivery.pickupDestination.location)
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
    ): RobotId {
        val delivery = deliveryRepository.getById(deliveryId)
        val previousRobotId = delivery.getAssignedRobotIdOrThrow()

        val previousRobot = robotRepository.getById(previousRobotId)
        val newRobot = robotRepository.getById(newRobotId)

        check(newRobot.isAvailable()) {
            "새 로봇이 배차 가능한 상태가 아닙니다."
        }

        val newRobotDestination = delivery.reassignRobot(newRobotId)

        previousRobot.unassignDelivery()
        newRobot.assignDelivery(deliveryId, newRobotDestination)

        deliveryRepository.save(delivery)
        robotRepository.save(previousRobot)
        robotRepository.save(newRobot)

        return previousRobotId
    }
}
