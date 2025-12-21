package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository

class DeliveryAssignmentService(
    private val robotRepository: RobotRepository,
    private val deliveryRepository: DeliveryRepository,
) {
    fun assignNearestRobot(delivery: Delivery): Robot? {
        val pickupLocation = delivery.pickupDestination.location

        val nearestRobot =
            robotRepository
                .findAvailableRobots()
                .minByOrNull { robot -> robot.location.distanceTo(pickupLocation) }
                ?: return null

        delivery.assignRobot(nearestRobot.getRobotId())
        nearestRobot.assignDelivery(delivery.getDeliveryId())

        return nearestRobot
    }
}
