package com.robotdelivery.application.eventhandler

import com.robotdelivery.config.IntegrationTestSupport
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.delivery.DestinationType
import com.robotdelivery.domain.delivery.event.DeliveryApproachingEvent
import com.robotdelivery.domain.delivery.event.DeliveryCanceledEvent
import com.robotdelivery.domain.delivery.event.DeliveryCompletedEvent
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import com.robotdelivery.domain.delivery.event.DeliveryEventHistoryRepository
import com.robotdelivery.domain.delivery.event.DeliveryRobotAssignedEvent
import com.robotdelivery.domain.delivery.event.DeliveryStartedEvent
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("DeliveryEventHistoryHandler 테스트")
class DeliveryEventHistoryHandlerTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var deliveryEventHistoryRepository: DeliveryEventHistoryRepository

    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var robotRepository: RobotRepository

    @Autowired
    private lateinit var handler: DeliveryEventHistoryHandler

    @BeforeEach
    fun setUp() {
        deliveryEventHistoryRepository.deleteAll()
        deliveryRepository.deleteAll()
        robotRepository.deleteAll()
    }

    private fun saveDelivery(): Delivery {
        val delivery =
            Delivery(
                pickupDestination =
                    Destination(
                        address = "서울시 중구 세종대로 110",
                        location = Location(latitude = 37.5665, longitude = 126.9780),
                    ),
                deliveryDestination =
                    Destination(
                        address = "서울시 강남구 테헤란로 1",
                        location = Location(latitude = 37.4979, longitude = 127.0276),
                    ),
                phoneNumber = "010-1234-5678",
            )
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveRobot(): Robot {
        val robot =
            Robot(
                name = "로봇-1",
                status = RobotStatus.OFF_DUTY,
                battery = 100,
                location = Location(latitude = 37.5665, longitude = 126.9780),
            )
        val savedRobot = robotRepository.saveAndFlush(robot)
        savedRobot.startDuty()
        savedRobot.pullDomainEvents()
        return robotRepository.saveAndFlush(savedRobot)
    }

    @Test
    @DisplayName("DeliveryCreatedEvent를 받으면 이력을 저장한다")
    fun `DeliveryCreatedEvent를 받으면 이력을 저장한다`() {
        val delivery = saveDelivery()
        val event =
            DeliveryCreatedEvent(
                deliveryId = delivery.getDeliveryId(),
                pickupLocation = Location(37.5665, 126.9780),
                deliveryLocation = Location(37.4979, 127.0276),
            )

        handler.handle(event)

        val histories = deliveryEventHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        val savedHistory = histories.first()
        assertThat(savedHistory.deliveryId).isEqualTo(delivery.getDeliveryId().value)
        assertThat(savedHistory.eventType).isEqualTo("DeliveryCreatedEvent")
        assertThat(savedHistory.robotId).isNull()
    }

    @Test
    @DisplayName("DeliveryRobotAssignedEvent를 받으면 로봇 ID와 함께 이력을 저장한다")
    fun `DeliveryRobotAssignedEvent를 받으면 로봇 ID와 함께 이력을 저장한다`() {
        val delivery = saveDelivery()
        val robot = saveRobot()
        val event =
            DeliveryRobotAssignedEvent(
                deliveryId = delivery.getDeliveryId(),
                robotId = robot.getRobotId(),
                pickupLocation = Location(37.5665, 126.9780),
            )

        handler.handle(event)

        val histories = deliveryEventHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        val savedHistory = histories.first()
        assertThat(savedHistory.deliveryId).isEqualTo(delivery.getDeliveryId().value)
        assertThat(savedHistory.eventType).isEqualTo("DeliveryRobotAssignedEvent")
        assertThat(savedHistory.robotId).isEqualTo(robot.getRobotId().value)
    }

    @Test
    @DisplayName("DeliveryStartedEvent를 받으면 이력을 저장한다")
    fun `DeliveryStartedEvent를 받으면 이력을 저장한다`() {
        val delivery = saveDelivery()
        val robot = saveRobot()
        val event =
            DeliveryStartedEvent(
                deliveryId = delivery.getDeliveryId(),
                robotId = robot.getRobotId(),
            )

        handler.handle(event)

        val histories = deliveryEventHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        val savedHistory = histories.first()
        assertThat(savedHistory.deliveryId).isEqualTo(delivery.getDeliveryId().value)
        assertThat(savedHistory.eventType).isEqualTo("DeliveryStartedEvent")
        assertThat(savedHistory.robotId).isEqualTo(robot.getRobotId().value)
    }

    @Test
    @DisplayName("DeliveryCompletedEvent를 받으면 이력을 저장한다")
    fun `DeliveryCompletedEvent를 받으면 이력을 저장한다`() {
        val delivery = saveDelivery()
        val robot = saveRobot()
        val event =
            DeliveryCompletedEvent(
                deliveryId = delivery.getDeliveryId(),
                robotId = robot.getRobotId(),
            )

        handler.handle(event)

        val histories = deliveryEventHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        val savedHistory = histories.first()
        assertThat(savedHistory.deliveryId).isEqualTo(delivery.getDeliveryId().value)
        assertThat(savedHistory.eventType).isEqualTo("DeliveryCompletedEvent")
        assertThat(savedHistory.robotId).isEqualTo(robot.getRobotId().value)
    }

    @Test
    @DisplayName("DeliveryCanceledEvent를 받으면 이력을 저장한다")
    fun `DeliveryCanceledEvent를 받으면 이력을 저장한다`() {
        val delivery = saveDelivery()
        val robot = saveRobot()
        val event =
            DeliveryCanceledEvent(
                deliveryId = delivery.getDeliveryId(),
                robotId = robot.getRobotId(),
                requiresReturn = true,
            )

        handler.handle(event)

        val histories = deliveryEventHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        val savedHistory = histories.first()
        assertThat(savedHistory.deliveryId).isEqualTo(delivery.getDeliveryId().value)
        assertThat(savedHistory.eventType).isEqualTo("DeliveryCanceledEvent")
        assertThat(savedHistory.robotId).isEqualTo(robot.getRobotId().value)
    }

    @Test
    @DisplayName("DeliveryCanceledEvent에 로봇 ID가 없으면 null로 저장한다")
    fun `DeliveryCanceledEvent에 로봇 ID가 없으면 null로 저장한다`() {
        val delivery = saveDelivery()
        val event =
            DeliveryCanceledEvent(
                deliveryId = delivery.getDeliveryId(),
                robotId = null,
                requiresReturn = false,
            )

        handler.handle(event)

        val histories = deliveryEventHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        val savedHistory = histories.first()
        assertThat(savedHistory.deliveryId).isEqualTo(delivery.getDeliveryId().value)
        assertThat(savedHistory.eventType).isEqualTo("DeliveryCanceledEvent")
        assertThat(savedHistory.robotId).isNull()
    }

    @Test
    @DisplayName("DeliveryApproachingEvent를 받으면 이력을 저장한다")
    fun `DeliveryApproachingEvent를 받으면 이력을 저장한다`() {
        val delivery = saveDelivery()
        val robot = saveRobot()
        val event =
            DeliveryApproachingEvent(
                deliveryId = delivery.getDeliveryId(),
                robotId = robot.getRobotId(),
                destination = Location(37.5665, 126.9780),
                destinationType = DestinationType.PICKUP,
            )

        handler.handle(event)

        val histories = deliveryEventHistoryRepository.findAll()
        assertThat(histories).hasSize(1)
        val savedHistory = histories.first()
        assertThat(savedHistory.deliveryId).isEqualTo(delivery.getDeliveryId().value)
        assertThat(savedHistory.eventType).isEqualTo("DeliveryApproachingEvent")
        assertThat(savedHistory.robotId).isEqualTo(robot.getRobotId().value)
    }
}
