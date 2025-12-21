package com.robotdelivery.application.command.delivery

import com.robotdelivery.application.command.Command
import com.robotdelivery.application.command.CommandHandler
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.DeliveryAssignmentService
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.robot.RobotRepository

data class AssignRobotCommand(
    val deliveryId: DeliveryId,
) : Command

data class AssignRobotResult(
    val deliveryId: DeliveryId,
    val assignedRobotId: RobotId?,
    val success: Boolean,
)

class AssignRobotCommandHandler(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val assignmentService: DeliveryAssignmentService,
    private val eventPublisher: DomainEventPublisher,
) : CommandHandler<AssignRobotCommand, AssignRobotResult> {
    override fun handle(command: AssignRobotCommand): AssignRobotResult {
        val delivery =
            deliveryRepository.findById(command.deliveryId)
                ?: throw IllegalArgumentException("배달을 찾을 수 없습니다: ${command.deliveryId}")

        val robot = assignmentService.assignNearestRobot(delivery)

        if (robot != null) {
            deliveryRepository.save(delivery)
            robotRepository.save(robot)

            eventPublisher.publishAll(delivery.pullDomainEvents())
            eventPublisher.publishAll(robot.pullDomainEvents())
        }

        return AssignRobotResult(
            deliveryId = command.deliveryId,
            assignedRobotId = robot?.getRobotId(),
            success = robot != null,
        )
    }
}
