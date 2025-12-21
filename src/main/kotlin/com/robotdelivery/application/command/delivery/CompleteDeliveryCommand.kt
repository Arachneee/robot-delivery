package com.robotdelivery.application.command.delivery

import com.robotdelivery.application.command.Command
import com.robotdelivery.application.command.CommandHandler
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.robot.RobotRepository

data class CompleteDeliveryCommand(
    val deliveryId: DeliveryId,
) : Command

class CompleteDeliveryCommandHandler(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val eventPublisher: DomainEventPublisher,
) : CommandHandler<CompleteDeliveryCommand, Unit> {

    override fun handle(command: CompleteDeliveryCommand) {
        val delivery = deliveryRepository.findById(command.deliveryId)
            ?: throw IllegalArgumentException("배달을 찾을 수 없습니다: ${command.deliveryId}")

        val robotId = delivery.assignedRobotId
            ?: throw IllegalStateException("배차된 로봇이 없습니다.")

        val robot = robotRepository.findById(robotId)
            ?: throw IllegalStateException("로봇을 찾을 수 없습니다: $robotId")

        delivery.complete()
        robot.completeDelivery()

        deliveryRepository.save(delivery)
        robotRepository.save(robot)

        eventPublisher.publishAll(delivery.pullDomainEvents())
        eventPublisher.publishAll(robot.pullDomainEvents())
    }
}

