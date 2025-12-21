package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.delivery.DeliveryAssignmentService
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent

class RobotBecameAvailableEventHandler(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val assignmentService: DeliveryAssignmentService,
    private val eventPublisher: DomainEventPublisher,
) {
    fun handle(event: RobotBecameAvailableEvent) {
        val pendingDeliveries = deliveryRepository.findPendingDeliveries()
        if (pendingDeliveries.isEmpty()) return

        val robot = robotRepository.findById(event.robotId) ?: return
        if (!robot.isAvailable()) return

        val nearestDelivery =
            pendingDeliveries
                .minByOrNull { it.pickupDestination.location.distanceTo(robot.location) }
                ?: return

        nearestDelivery.assignRobot(robot.getRobotId())
        robot.assignDelivery(nearestDelivery.getDeliveryId())

        deliveryRepository.save(nearestDelivery)
        robotRepository.save(robot)

        eventPublisher.publishAll(nearestDelivery.pullDomainEvents())
        eventPublisher.publishAll(robot.pullDomainEvents())
    }
}
