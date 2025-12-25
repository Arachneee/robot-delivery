package com.robotdelivery.domain.delivery

import com.robotdelivery.application.query.RobotQueryService
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.findById

class DeliveryAssignmentService(
    private val robotRepository: RobotRepository,
    private val deliveryRepository: DeliveryRepository,
    private val robotQueryService: RobotQueryService,
) {
    fun assignNearestRobotToDelivery(delivery: Delivery): Robot? {
        val pickupLocation = delivery.pickupDestination.location
        val nearestRobotId =
            robotQueryService.findNearestAvailableRobotId(pickupLocation, MINIMUM_BATTERY)
                ?: return null
        val nearestRobot = robotRepository.findById(nearestRobotId) ?: return null

        delivery.assignRobot(nearestRobot.getRobotId())
        nearestRobot.assignDelivery(delivery.getDeliveryId(), pickupLocation)

        return nearestRobot
    }

    fun assignNearestDeliveryToRobot(robot: Robot): Delivery? {
        val iotState = robotQueryService.findIotState(robot.getRobotId()) ?: return null
        if (!robot.isAvailableForDelivery() || !iotState.hasSufficientBattery(MINIMUM_BATTERY)) return null

        val nearestDelivery =
            deliveryRepository
                .findAllByStatus(DeliveryStatus.PENDING)
                .minByOrNull { it.pickupDestination.location.distanceTo(iotState.location) }
                ?: return null

        nearestDelivery.assignRobot(robot.getRobotId())
        robot.assignDelivery(nearestDelivery.getDeliveryId(), nearestDelivery.pickupDestination.location)

        return nearestDelivery
    }

    companion object {
        private const val MINIMUM_BATTERY = 20
    }
}
