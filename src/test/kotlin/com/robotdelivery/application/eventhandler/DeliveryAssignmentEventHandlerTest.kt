package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryAssignmentService
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("DeliveryAssignmentEventHandler 테스트")
class DeliveryAssignmentEventHandlerTest {
    @Mock
    private lateinit var deliveryRepository: DeliveryRepository

    @Mock
    private lateinit var robotRepository: RobotRepository

    @Mock
    private lateinit var assignmentService: DeliveryAssignmentService

    private lateinit var eventHandler: DeliveryAssignmentEventHandler

    @BeforeEach
    fun setUp() {
        eventHandler = DeliveryAssignmentEventHandler(deliveryRepository, robotRepository, assignmentService)
    }

    private fun createDelivery(
        id: Long = 1L,
        pickupLocation: Location = Location(latitude = 37.5665, longitude = 126.9780),
    ): Delivery =
        Delivery(
            id = id,
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

    private fun createRobot(
        id: Long = 1L,
        name: String = "로봇-1",
        location: Location = Location(latitude = 37.5665, longitude = 126.9780),
    ): Robot {
        val robot =
            Robot(
                id = id,
                name = name,
                status = RobotStatus.OFF_DUTY,
                battery = 100,
                location = location,
            )
        robot.startDuty()
        robot.pullDomainEvents()
        return robot
    }

    @Nested
    @DisplayName("DeliveryCreatedEvent 핸들러 테스트")
    inner class HandleDeliveryCreatedEventTest {
        @Test
        @DisplayName("배달이 생성되면 가장 가까운 로봇이 배정된다")
        fun `배달이 생성되면 가장 가까운 로봇이 배정된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation = pickupLocation)
            val robot = createRobot(location = Location(37.5660, 126.9770))
            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = pickupLocation,
                    deliveryLocation = Location(37.4979, 127.0276),
                )

            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(assignmentService.assignNearestRobotToDelivery(delivery)).thenAnswer {
                delivery.assignRobot(robot.getRobotId())
                robot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)
                robot
            }

            eventHandler.handle(event)

            assertEquals(DeliveryStatus.ASSIGNED, delivery.status)
            assertEquals(robot.getRobotId(), delivery.assignedRobotId)
            verify(deliveryRepository).save(delivery)
            verify(robotRepository).save(robot)
        }

        @Test
        @DisplayName("사용 가능한 로봇이 없으면 배정되지 않는다")
        fun `사용 가능한 로봇이 없으면 배정되지 않는다`() {
            val delivery = createDelivery()
            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = delivery.pickupDestination.location,
                    deliveryLocation = delivery.deliveryDestination.location,
                )

            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(assignmentService.assignNearestRobotToDelivery(delivery)).thenReturn(null)

            eventHandler.handle(event)

            assertEquals(DeliveryStatus.PENDING, delivery.status)
            assertNull(delivery.assignedRobotId)
            verify(deliveryRepository, never()).save(any())
            verify(robotRepository, never()).save(any())
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

            whenever(deliveryRepository.findById(DeliveryId(99999L))).thenReturn(null)

            eventHandler.handle(event)

            verify(deliveryRepository, never()).save(any())
            verify(robotRepository, never()).save(any())
        }

        @Test
        @DisplayName("여러 로봇 중 가장 가까운 로봇이 선택된다")
        fun `여러 로봇 중 가장 가까운 로봇이 선택된다`() {
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val delivery = createDelivery(pickupLocation = pickupLocation)
            val nearRobot = createRobot(id = 2L, name = "로봇-near", location = Location(37.5005, 127.0005))
            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = pickupLocation,
                    deliveryLocation = delivery.deliveryDestination.location,
                )

            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(assignmentService.assignNearestRobotToDelivery(delivery)).thenAnswer {
                delivery.assignRobot(nearRobot.getRobotId())
                nearRobot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)
                nearRobot
            }

            eventHandler.handle(event)

            assertEquals(nearRobot.getRobotId(), delivery.assignedRobotId)
        }
    }

    @Nested
    @DisplayName("RobotBecameAvailableEvent 핸들러 테스트")
    inner class HandleRobotBecameAvailableEventTest {
        @Test
        @DisplayName("로봇이 가용해지면 대기 중인 가장 가까운 배달이 배정된다")
        fun `로봇이 가용해지면 대기 중인 가장 가까운 배달이 배정된다`() {
            val robotLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val robot = createRobot(location = robotLocation)
            val delivery = createDelivery(pickupLocation = Location(37.5660, 126.9770))
            val event =
                RobotBecameAvailableEvent(
                    robotId = robot.getRobotId(),
                    location = robotLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(assignmentService.assignNearestDeliveryToRobot(robot)).thenAnswer {
                delivery.assignRobot(robot.getRobotId())
                robot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)
                delivery
            }

            eventHandler.handle(event)

            assertEquals(DeliveryStatus.ASSIGNED, delivery.status)
            assertEquals(robot.getRobotId(), delivery.assignedRobotId)
            assertEquals(RobotStatus.BUSY, robot.status)
            assertEquals(delivery.getDeliveryId(), robot.currentDeliveryId)
            verify(deliveryRepository).save(delivery)
            verify(robotRepository).save(robot)
        }

        @Test
        @DisplayName("대기 중인 배달이 없으면 배정되지 않는다")
        fun `대기 중인 배달이 없으면 배정되지 않는다`() {
            val robot = createRobot()
            val event =
                RobotBecameAvailableEvent(
                    robotId = robot.getRobotId(),
                    location = robot.location,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(assignmentService.assignNearestDeliveryToRobot(robot)).thenReturn(null)

            eventHandler.handle(event)

            assertEquals(RobotStatus.READY, robot.status)
            assertNull(robot.currentDeliveryId)
            verify(deliveryRepository, never()).save(any())
            verify(robotRepository, never()).save(any())
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 무시된다`() {
            val event =
                RobotBecameAvailableEvent(
                    robotId = RobotId(99999L),
                    location = Location(37.5665, 126.9780),
                )

            whenever(robotRepository.findById(RobotId(99999L))).thenReturn(null)

            eventHandler.handle(event)

            verify(deliveryRepository, never()).save(any())
            verify(robotRepository, never()).save(any())
        }

        @Test
        @DisplayName("로봇이 가용 상태가 아니면 배정되지 않는다")
        fun `로봇이 가용 상태가 아니면 배정되지 않는다`() {
            val robot =
                Robot(
                    id = 1L,
                    name = "로봇-1",
                    status = RobotStatus.OFF_DUTY,
                    battery = 100,
                    location = Location(37.5665, 126.9780),
                )
            val event =
                RobotBecameAvailableEvent(
                    robotId = robot.getRobotId(),
                    location = robot.location,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(assignmentService.assignNearestDeliveryToRobot(robot)).thenReturn(null)

            eventHandler.handle(event)

            verify(deliveryRepository, never()).save(any())
            verify(robotRepository, never()).save(any())
        }

        @Test
        @DisplayName("여러 배달 중 가장 가까운 배달이 선택된다")
        fun `여러 배달 중 가장 가까운 배달이 선택된다`() {
            val robotLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val robot = createRobot(location = robotLocation)
            val nearDelivery = createDelivery(id = 2L, pickupLocation = Location(37.5005, 127.0005))
            val event =
                RobotBecameAvailableEvent(
                    robotId = robot.getRobotId(),
                    location = robotLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(assignmentService.assignNearestDeliveryToRobot(robot)).thenAnswer {
                nearDelivery.assignRobot(robot.getRobotId())
                robot.assignDelivery(nearDelivery.getDeliveryId(), nearDelivery.pickupDestination.location)
                nearDelivery
            }

            eventHandler.handle(event)

            assertEquals(nearDelivery.getDeliveryId(), robot.currentDeliveryId)
        }
    }
}
