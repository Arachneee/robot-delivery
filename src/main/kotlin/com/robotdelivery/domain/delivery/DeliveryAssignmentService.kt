package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.delivery.vo.AssignmentResult
import com.robotdelivery.domain.delivery.vo.DeliveryStatus
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotAvailabilityService
import com.robotdelivery.domain.robot.RobotMapClient

class DeliveryAssignmentService(
    private val deliveryRepository: DeliveryRepository,
    private val robotAvailabilityService: RobotAvailabilityService,
    private val robotMapClient: RobotMapClient,
) {
    fun assignNearestRobotToDelivery(delivery: Delivery): Robot? {
        val pickupLocation = delivery.pickupDestination.location
        val deliveryLocation = delivery.deliveryDestination.location

        val candidates =
            robotAvailabilityService
                .findAvailableCandidatesSortedByDistance(pickupLocation, MINIMUM_BATTERY)

        for (candidate in candidates) {
            val waypoints = listOf(candidate.currentLocation, pickupLocation, deliveryLocation)
            val routeResult = robotMapClient.findRoute(waypoints)
            if (!routeResult.isAvailable) continue

            val robot = candidate.robot
            delivery.assignRobot(robot.getRobotId(), routeResult)
            robot.assignDelivery(delivery.getDeliveryId(), pickupLocation)
            return robot
        }

        return null
    }

    fun assignNearestDeliveryToRobot(robot: Robot): Delivery? {
        val iotState = robotAvailabilityService.findIotState(robot.getRobotId()) ?: return null
        if (!robot.isAvailableForDelivery() || !iotState.hasSufficientBattery(MINIMUM_BATTERY)) return null

        val pendingDeliveries =
            deliveryRepository
                .findAllByStatus(DeliveryStatus.PENDING)
                .sortedBy { it.pickupDestination.location.distanceTo(iotState.location) }

        for (delivery in pendingDeliveries) {
            val waypoints =
                listOf(
                    iotState.location,
                    delivery.pickupDestination.location,
                    delivery.deliveryDestination.location,
                )
            val routeResult = robotMapClient.findRoute(waypoints)
            if (!routeResult.isAvailable) continue

            delivery.assignRobot(robot.getRobotId(), routeResult)
            robot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)
            return delivery
        }

        return null
    }

    fun assignSpecificRobotToDelivery(
        delivery: Delivery,
        newRobot: Robot,
        previousRobot: Robot? = null,
    ): AssignmentResult {
        if (!newRobot.isAvailableForDelivery()) return AssignmentResult.Failure.RobotNotAvailable
        val iotState =
            robotAvailabilityService.findIotState(newRobot.getRobotId())
                ?: return AssignmentResult.Failure.IotStateNotFound
        if (!iotState.hasSufficientBattery(MINIMUM_BATTERY)) return AssignmentResult.Failure.InsufficientBattery

        val waypoints =
            listOf(
                iotState.location,
                delivery.pickupDestination.location,
                delivery.deliveryDestination.location,
            )
        val routeResult = robotMapClient.findRoute(waypoints)
        if (!routeResult.isAvailable) return AssignmentResult.Failure.RouteNotAvailable

        if (delivery.assignedRobotId != null) {
            delivery.reassignRobot(newRobot.getRobotId(), routeResult)
        } else {
            delivery.assignRobot(newRobot.getRobotId(), routeResult)
        }
        newRobot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)
        previousRobot?.unassignDelivery()
        return AssignmentResult.Success
    }

    companion object {
        private const val MINIMUM_BATTERY = 20
    }
}
