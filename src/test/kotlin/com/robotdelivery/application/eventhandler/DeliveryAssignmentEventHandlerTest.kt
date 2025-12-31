package com.robotdelivery.application.eventhandler

import com.robotdelivery.config.IntegrationTestSupport
import com.robotdelivery.domain.common.vo.DeliveryId
import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import com.robotdelivery.domain.delivery.vo.DeliveryStatus
import com.robotdelivery.domain.delivery.vo.Destination
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotIotState
import com.robotdelivery.domain.robot.RobotIotStateRepository
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent
import com.robotdelivery.domain.robot.findById
import com.robotdelivery.domain.robot.vo.RobotStatus
import com.robotdelivery.infrastructure.persistence.InMemoryRobotIotStateRepository
import org.assertj.core.api.Assertions.assertThat
import java.time.Duration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("DeliveryAssignmentEventHandler 테스트")
class DeliveryAssignmentEventHandlerTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var robotRepository: RobotRepository

    @Autowired
    private lateinit var iotStateRepository: RobotIotStateRepository

    @Autowired
    private lateinit var eventHandler: DeliveryAssignmentEventHandler

    @BeforeEach
    fun setUp() {
        deliveryRepository.deleteAll()
        robotRepository.deleteAll()
        (iotStateRepository as InMemoryRobotIotStateRepository).deleteAll()
    }

    private fun saveDelivery(pickupLocation: Location = Location(latitude = 37.5665, longitude = 126.9780)): Delivery {
        val delivery =
            Delivery(
                pickupDestination =
                    Destination(
                        address = "서울시 중구 세종대로 110",
                        location = pickupLocation,
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

    private fun saveRobot(
        name: String = "로봇-1",
        location: Location = Location(latitude = 37.5665, longitude = 126.9780),
        battery: Int = 100,
    ): Robot {
        val robot = Robot(name = name, status = RobotStatus.OFF_DUTY)
        val savedRobot = robotRepository.saveAndFlush(robot)

        iotStateRepository.save(
            RobotIotState(
                robotId = savedRobot.getRobotId(),
                location = location,
                battery = battery,
                doorOpen = false,
                loadWeight = 0.0,
            ),
        )

        return savedRobot
    }

    private fun saveReadyRobot(
        name: String = "로봇-1",
        location: Location = Location(latitude = 37.5665, longitude = 126.9780),
        battery: Int = 100,
    ): Robot {
        val robot = saveRobot(name, location, battery)
        robot.startDuty()
        robot.pullDomainEvents()
        return robotRepository.saveAndFlush(robot)
    }

    @Nested
    @DisplayName("DeliveryCreatedEvent 핸들러 테스트")
    inner class HandleDeliveryCreatedEventTest {
        @Test
        @DisplayName("배달이 생성되면 가장 가까운 로봇이 배정된다")
        fun `배달이 생성되면 가장 가까운 로봇이 배정된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = saveDelivery(pickupLocation = pickupLocation)
            val robot = saveReadyRobot(location = Location(37.5660, 126.9770))
            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = pickupLocation,
                    deliveryLocation = Location(37.4979, 127.0276),
                )

            eventHandler.handle(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.ASSIGNED)
            assertThat(updatedDelivery.assignedRobotId).isEqualTo(robot.getRobotId())
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.BUSY)
            assertThat(updatedRobot.currentDeliveryId).isEqualTo(delivery.getDeliveryId())
        }

        @Test
        @DisplayName("사용 가능한 로봇이 없으면 배정되지 않는다")
        fun `사용 가능한 로봇이 없으면 배정되지 않는다`() {
            val delivery = saveDelivery()
            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = delivery.pickupDestination.location,
                    deliveryLocation = delivery.deliveryDestination.location,
                )

            eventHandler.handle(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(updatedDelivery.assignedRobotId).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 배달 ID로 이벤트가 오면 무시된다`() {
            val event =
                DeliveryCreatedEvent(
                    deliveryId = DeliveryId(99999L),
                    pickupLocation = Location(37.5665, 126.9780),
                    deliveryLocation = Location(37.4979, 127.0276),
                )

            eventHandler.handle(event)
        }

        @Test
        @DisplayName("여러 로봇 중 가장 가까운 로봇이 선택된다")
        fun `여러 로봇 중 가장 가까운 로봇이 선택된다`() {
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val delivery = saveDelivery(pickupLocation = pickupLocation)
            saveReadyRobot(name = "로봇-far", location = Location(37.6000, 127.1000))
            val nearRobot = saveReadyRobot(name = "로봇-near", location = Location(37.5005, 127.0005))
            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = pickupLocation,
                    deliveryLocation = delivery.deliveryDestination.location,
                )

            eventHandler.handle(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.assignedRobotId).isEqualTo(nearRobot.getRobotId())
        }
    }

    @Nested
    @DisplayName("RobotBecameAvailableEvent 핸들러 테스트")
    inner class HandleRobotBecameAvailableEventTest {
        @Test
        @DisplayName("로봇이 가용해지면 대기 중인 가장 가까운 배달이 배정된다")
        fun `로봇이 가용해지면 대기 중인 가장 가까운 배달이 배정된다`() {
            val robotLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val robot = saveReadyRobot(location = robotLocation)
            val delivery = saveDelivery(pickupLocation = Location(37.5660, 126.9770))
            val event = RobotBecameAvailableEvent(robotId = robot.getRobotId())

            eventHandler.handle(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.ASSIGNED)
            assertThat(updatedDelivery.assignedRobotId).isEqualTo(robot.getRobotId())
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.BUSY)
            assertThat(updatedRobot.currentDeliveryId).isEqualTo(delivery.getDeliveryId())
        }

        @Test
        @DisplayName("대기 중인 배달이 없으면 배정되지 않는다")
        fun `대기 중인 배달이 없으면 배정되지 않는다`() {
            val robot = saveReadyRobot()
            val event = RobotBecameAvailableEvent(robotId = robot.getRobotId())

            eventHandler.handle(event)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
            assertThat(updatedRobot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 무시된다`() {
            saveDelivery()
            val event =
                RobotBecameAvailableEvent(
                    robotId =
                        RobotId(99999L),
                )

            eventHandler.handle(event)
        }

        @Test
        @DisplayName("로봇이 가용 상태가 아니면 배정되지 않는다")
        fun `로봇이 가용 상태가 아니면 배정되지 않는다`() {
            val robot = saveRobot()
            saveDelivery()
            val event = RobotBecameAvailableEvent(robotId = robot.getRobotId())

            eventHandler.handle(event)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.OFF_DUTY)
            assertThat(updatedRobot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("여러 배달 중 가장 가까운 배달이 선택된다")
        fun `여러 배달 중 가장 가까운 배달이 선택된다`() {
            val robotLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val robot = saveReadyRobot(location = robotLocation)
            saveDelivery(pickupLocation = Location(37.6000, 127.1000))
            val nearDelivery = saveDelivery(pickupLocation = Location(37.5005, 127.0005))
            val event = RobotBecameAvailableEvent(robotId = robot.getRobotId())

            eventHandler.handle(event)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.currentDeliveryId).isEqualTo(nearDelivery.getDeliveryId())
        }
    }
}
