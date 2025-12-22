package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus

class DeliveryAssignmentService(
    private val robotRepository: RobotRepository,
    private val deliveryRepository: DeliveryRepository,
) {
    fun assignNearestRobotToDelivery(delivery: Delivery): Robot? {
        val nearestRobot =
            robotRepository
                .findAllByStatus(RobotStatus.READY)
                .filter { it.isAvailable() }
                .minByOrNull { it.location.distanceTo(delivery.pickupDestination.location) }
                ?: return null

        delivery.assignRobot(nearestRobot.getRobotId())
        nearestRobot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)

        return nearestRobot
    }

    fun assignNearestDeliveryToRobot(robot: Robot): Delivery? {
        if (!robot.isAvailable()) return null

        val nearestDelivery =
            deliveryRepository
                .findAllByStatus(DeliveryStatus.PENDING)
                .minByOrNull { it.pickupDestination.location.distanceTo(robot.location) }
                ?: return null

        nearestDelivery.assignRobot(robot.getRobotId())
        robot.assignDelivery(nearestDelivery.getDeliveryId(), nearestDelivery.pickupDestination.location)

        return nearestDelivery
    }
}
