package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import com.robotdelivery.domain.robot.RobotRepository

class DeliveryCreatedEventHandler(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val eventPublisher: DomainEventPublisher,
) {
    fun handle(event: DeliveryCreatedEvent) {
        val delivery = deliveryRepository.findById(event.deliveryId) ?: return

        val availableRobots = robotRepository.findAvailableRobots()
        if (availableRobots.isEmpty()) return

        val nearestRobot =
            availableRobots
                .minByOrNull { it.location.distanceTo(event.pickupLocation) }
                ?: return

        delivery.assignRobot(nearestRobot.getRobotId())
        nearestRobot.assignDelivery(delivery.getDeliveryId())

        deliveryRepository.save(delivery)
        robotRepository.save(nearestRobot)

        eventPublisher.publishAll(delivery.pullDomainEvents())
        eventPublisher.publishAll(nearestRobot.pullDomainEvents())
    }
}
