package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryAssignmentService
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DeliveryAssignmentEventHandler(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val assignmentService: DeliveryAssignmentService,
    private val eventPublisher: DomainEventPublisher,
) {
    @TransactionalEventListener
    fun handle(event: DeliveryCreatedEvent) {
        val delivery = deliveryRepository.findById(event.deliveryId) ?: return
        val robot = assignmentService.assignNearestRobotToDelivery(delivery) ?: return

        saveAndPublishEvents(delivery, robot)
    }

    @TransactionalEventListener
    fun handle(event: RobotBecameAvailableEvent) {
        val robot = robotRepository.findById(event.robotId) ?: return
        val delivery = assignmentService.assignNearestDeliveryToRobot(robot) ?: return

        saveAndPublishEvents(delivery, robot)
    }

    private fun saveAndPublishEvents(
        delivery: Delivery,
        robot: Robot,
    ) {
        deliveryRepository.save(delivery)
        robotRepository.save(robot)

        eventPublisher.publishAll(delivery.pullDomainEvents())
        eventPublisher.publishAll(robot.pullDomainEvents())
    }
}
