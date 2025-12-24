package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.delivery.DestinationType
import com.robotdelivery.domain.delivery.event.DeliveryApproachingEvent
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.domain.robot.event.RobotApproachingEvent
import com.robotdelivery.domain.robot.event.RobotArrivedEvent
import org.assertj.core.api.Assertions.assertThat
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
@DisplayName("DeliveryRobotEventHandler 테스트")
class DeliveryRobotEventHandlerTest {
    @Mock
    private lateinit var deliveryRepository: DeliveryRepository

    @Mock
    private lateinit var robotRepository: RobotRepository

    private lateinit var eventHandler: DeliveryRobotEventHandler

    @BeforeEach
    fun setUp() {
        eventHandler = DeliveryRobotEventHandler(deliveryRepository, robotRepository)
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
        deliveryId: DeliveryId? = null,
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
        if (deliveryId != null) {
            robot.assignDelivery(deliveryId, location)
        }
        robot.pullDomainEvents()
        return robot
    }

    @Nested
    @DisplayName("RobotApproachingEvent 핸들러 테스트")
    inner class HandleRobotApproachingEventTest {
        @Test
        @DisplayName("ASSIGNED 상태에서 접근 이벤트가 오면 DeliveryApproachingEvent가 등록된다")
        fun `ASSIGNED 상태에서 접근 이벤트가 오면 DeliveryApproachingEvent가 등록된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation = pickupLocation)
            delivery.assignRobot(RobotId(1L))
            delivery.pullDomainEvents()

            val robot = createRobot(deliveryId = delivery.getDeliveryId())
            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = pickupLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            eventHandler.handleApproaching(event)

            verify(deliveryRepository).save(delivery)

            val domainEvents = delivery.pullDomainEvents()
            assertThat(domainEvents).hasSize(1)
            val approachingEvent = domainEvents.first() as DeliveryApproachingEvent
            assertThat(approachingEvent.deliveryId).isEqualTo(delivery.getDeliveryId())
            assertThat(approachingEvent.robotId).isEqualTo(robot.getRobotId())
            assertThat(approachingEvent.destinationType).isEqualTo(DestinationType.PICKUP)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 접근 이벤트가 오면 DELIVERY 타입의 DeliveryApproachingEvent가 등록된다")
        fun `DELIVERING 상태에서 접근 이벤트가 오면 DELIVERY 타입의 DeliveryApproachingEvent가 등록된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val deliveryLocation = Location(latitude = 37.4979, longitude = 127.0276)
            val delivery = createDelivery(pickupLocation = pickupLocation)
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.pullDomainEvents()

            val robot = createRobot(deliveryId = delivery.getDeliveryId(), location = deliveryLocation)
            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = deliveryLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            eventHandler.handleApproaching(event)

            verify(deliveryRepository).save(delivery)

            val domainEvents = delivery.pullDomainEvents()
            assertThat(domainEvents).hasSize(1)
            val approachingEvent = domainEvents.first() as DeliveryApproachingEvent
            assertThat(approachingEvent.destinationType).isEqualTo(DestinationType.DELIVERY)
        }

        @Test
        @DisplayName("RETURNING 상태에서 접근 이벤트가 오면 RETURN 타입의 DeliveryApproachingEvent가 등록된다")
        fun `RETURNING 상태에서 접근 이벤트가 오면 RETURN 타입의 DeliveryApproachingEvent가 등록된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation = pickupLocation)
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.cancel()
            delivery.pullDomainEvents()

            val robot = createRobot(deliveryId = delivery.getDeliveryId())
            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = pickupLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            eventHandler.handleApproaching(event)

            verify(deliveryRepository).save(delivery)

            val domainEvents = delivery.pullDomainEvents()
            assertThat(domainEvents).hasSize(1)
            val approachingEvent = domainEvents.first() as DeliveryApproachingEvent
            assertThat(approachingEvent.destinationType).isEqualTo(DestinationType.RETURN)
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 접근 이벤트가 오면 무시된다`() {
            val event =
                RobotApproachingEvent(
                    robotId = RobotId(99999L),
                    destination = Location(37.5665, 126.9780),
                )

            whenever(robotRepository.findById(RobotId(99999L))).thenReturn(null)

            eventHandler.handleApproaching(event)

            verify(deliveryRepository, never()).save(any())
        }

        @Test
        @DisplayName("로봇에 할당된 배달이 없으면 무시된다")
        fun `로봇에 할당된 배달이 없으면 접근 이벤트가 무시된다`() {
            val robot = createRobot(deliveryId = null)
            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = Location(37.5665, 126.9780),
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)

            eventHandler.handleApproaching(event)

            verify(deliveryRepository, never()).save(any())
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID가 할당된 경우 무시된다")
        fun `존재하지 않는 배달 ID가 할당된 경우 접근 이벤트가 무시된다`() {
            val delivery = createDelivery()
            val robot = createRobot(deliveryId = delivery.getDeliveryId())
            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = Location(37.5665, 126.9780),
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(null)

            eventHandler.handleApproaching(event)

            verify(deliveryRepository, never()).save(any())
        }
    }

    @Nested
    @DisplayName("RobotArrivedEvent 핸들러 테스트")
    inner class HandleRobotArrivedEventTest {
        @Test
        @DisplayName("ASSIGNED 상태의 배달이 도착하면 PICKUP_ARRIVED가 된다")
        fun `ASSIGNED 상태의 배달이 도착하면 PICKUP_ARRIVED가 된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation = pickupLocation)
            delivery.assignRobot(RobotId(1L))
            delivery.pullDomainEvents()

            val robot = createRobot(deliveryId = delivery.getDeliveryId())
            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = pickupLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            eventHandler.handleArrived(event)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PICKUP_ARRIVED)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("DELIVERING 상태의 배달이 도착하면 DELIVERY_ARRIVED가 된다")
        fun `DELIVERING 상태의 배달이 도착하면 DELIVERY_ARRIVED가 된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val deliveryLocation = Location(latitude = 37.4979, longitude = 127.0276)
            val delivery = createDelivery(pickupLocation = pickupLocation)
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.pullDomainEvents()

            val robot = createRobot(deliveryId = delivery.getDeliveryId(), location = deliveryLocation)
            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = deliveryLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            eventHandler.handleArrived(event)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.DELIVERY_ARRIVED)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("RETURNING 상태의 배달이 도착하면 RETURN_ARRIVED가 된다")
        fun `RETURNING 상태의 배달이 도착하면 RETURN_ARRIVED가 된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation = pickupLocation)
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.cancel()
            delivery.pullDomainEvents()

            val robot = createRobot(deliveryId = delivery.getDeliveryId())
            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = pickupLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            eventHandler.handleArrived(event)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.RETURN_ARRIVED)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 무시된다`() {
            val event =
                RobotArrivedEvent(
                    robotId = RobotId(99999L),
                    destination = Location(37.5665, 126.9780),
                )

            whenever(robotRepository.findById(RobotId(99999L))).thenReturn(null)

            eventHandler.handleArrived(event)

            verify(deliveryRepository, never()).save(any())
        }

        @Test
        @DisplayName("로봇에 할당된 배달이 없으면 무시된다")
        fun `로봇에 할당된 배달이 없으면 무시된다`() {
            val robot = createRobot(deliveryId = null)
            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = Location(37.5665, 126.9780),
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)

            eventHandler.handleArrived(event)

            verify(deliveryRepository, never()).save(any())
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID가 할당된 경우 무시된다")
        fun `존재하지 않는 배달 ID가 할당된 경우 무시된다`() {
            val delivery = createDelivery()
            val robot = createRobot(deliveryId = delivery.getDeliveryId())
            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = Location(37.5665, 126.9780),
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(null)

            eventHandler.handleArrived(event)

            verify(deliveryRepository, never()).save(any())
        }
    }
}
