package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryAssignmentService
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DeliveryAssignmentEventHandler(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val assignmentService: DeliveryAssignmentService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    fun dispatch(event: DeliveryCreatedEvent) {
        log.info("DeliveryCreatedEvent 수신: deliveryId={}", event.deliveryId)

        val delivery =
            deliveryRepository.findById(event.deliveryId) ?: run {
                log.warn("배달을 찾을 수 없음: deliveryId={}", event.deliveryId)
                return
            }
        val robot =
            assignmentService.assignNearestRobotToDelivery(delivery) ?: run {
                log.warn("배정 가능한 로봇 없음: deliveryId={}", event.deliveryId)
                return
            }

        log.info("로봇 배정 완료: delivery={}, robot={}", delivery, robot)
        saveAndPublishEvents(delivery, robot)
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    fun dispatch(event: RobotBecameAvailableEvent) {
        log.info("RobotBecameAvailableEvent 수신: robotId={}", event.robotId)

        val robot =
            robotRepository.findById(event.robotId) ?: run {
                log.warn("로봇을 찾을 수 없음: robotId={}", event.robotId)
                return
            }
        val delivery =
            assignmentService.assignNearestDeliveryToRobot(robot) ?: run {
                log.warn("배정 가능한 배달 없음: robotId={}", event.robotId)
                return
            }

        log.info("배달 배정 완료: robot={}, delivery={}", robot, delivery)
        saveAndPublishEvents(delivery, robot)
    }

    private fun saveAndPublishEvents(
        delivery: Delivery,
        robot: Robot,
    ) {
        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }
}
