package com.robotdelivery.application.eventhandler

import com.robotdelivery.config.IntegrationTestSupport
import com.robotdelivery.config.TestDeliveryApproachingEventListener
import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.vo.DeliveryStatus
import com.robotdelivery.domain.delivery.vo.Destination
import com.robotdelivery.domain.delivery.vo.DestinationType
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.event.RobotApproachingEvent
import com.robotdelivery.domain.robot.event.RobotArrivedEvent
import com.robotdelivery.domain.robot.findById
import com.robotdelivery.domain.robot.vo.RobotStatus
import com.robotdelivery.domain.robot.vo.RouteResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("DeliveryRobotEventHandler 테스트")
class DeliveryRobotEventHandlerTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var robotRepository: RobotRepository

    @Autowired
    private lateinit var eventHandler: DeliveryRobotEventHandler

    @Autowired
    private lateinit var testEventListener: TestDeliveryApproachingEventListener

    private val defaultRouteResult = RouteResult.of(toPickupSeconds = 300, toDeliverySeconds = 600)

    @BeforeEach
    fun setUp() {
        deliveryRepository.deleteAll()
        robotRepository.deleteAll()
        testEventListener.clear()
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

    private fun saveRobot(name: String = "로봇-1"): Robot {
        val robot = Robot(name = name, status = RobotStatus.OFF_DUTY)
        return robotRepository.saveAndFlush(robot)
    }

    private fun saveRobotWithDelivery(
        delivery: Delivery,
        pickupLocation: Location = Location(latitude = 37.5665, longitude = 126.9780),
    ): Robot {
        val robot = saveRobot()
        robot.startDuty()
        robot.assignDelivery(delivery.getDeliveryId(), pickupLocation)
        robot.pullDomainEvents()
        return robotRepository.saveAndFlush(robot)
    }

    private fun saveDeliveryInAssignedState(): Delivery {
        val delivery = saveDelivery()
        val robot = saveRobotWithDelivery(delivery)
        delivery.assignRobot(robot.getRobotId(), defaultRouteResult)
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInDeliveringState(): Delivery {
        val delivery = saveDeliveryInAssignedState()
        delivery.arrived()
        delivery.openDoor()
        delivery.startDelivery()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInReturningState(): Delivery {
        val delivery = saveDeliveryInDeliveringState()
        delivery.cancel()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun getAssignedRobot(delivery: Delivery): Robot {
        val robotId = delivery.assignedRobotId ?: throw IllegalStateException("배달에 로봇이 할당되어 있지 않습니다.")
        return robotRepository.findById(robotId) ?: throw IllegalStateException("로봇을 찾을 수 없습니다: $robotId")
    }

    @Nested
    @DisplayName("RobotApproachingEvent 핸들러 테스트")
    inner class HandleRobotApproachingEventTest {
        @Test
        @DisplayName("ASSIGNED 상태에서 접근 이벤트가 오면 DeliveryApproachingEvent가 등록된다")
        fun `ASSIGNED 상태에서 접근 이벤트가 오면 DeliveryApproachingEvent가 등록된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = saveDeliveryInAssignedState()
            val robot = getAssignedRobot(delivery)
            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = pickupLocation,
                )

            eventHandler.handleApproaching(event)

            val capturedEvents = testEventListener.getCapturedEvents()
            assertThat(capturedEvents).hasSize(1)
            val approachingEvent = capturedEvents.first()
            assertThat(approachingEvent.deliveryId).isEqualTo(delivery.getDeliveryId())
            assertThat(approachingEvent.robotId).isEqualTo(robot.getRobotId())
            assertThat(approachingEvent.destinationType).isEqualTo(DestinationType.PICKUP)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 접근 이벤트가 오면 DELIVERY 타입의 DeliveryApproachingEvent가 등록된다")
        fun `DELIVERING 상태에서 접근 이벤트가 오면 DELIVERY 타입의 DeliveryApproachingEvent가 등록된다`() {
            val deliveryLocation = Location(latitude = 37.4979, longitude = 127.0276)
            val delivery = saveDeliveryInDeliveringState()
            val robot = getAssignedRobot(delivery)
            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = deliveryLocation,
                )

            eventHandler.handleApproaching(event)

            val capturedEvents = testEventListener.getCapturedEvents()
            assertThat(capturedEvents).hasSize(1)
            val approachingEvent = capturedEvents.first()
            assertThat(approachingEvent.destinationType).isEqualTo(DestinationType.DELIVERY)
        }

        @Test
        @DisplayName("RETURNING 상태에서 접근 이벤트가 오면 RETURN 타입의 DeliveryApproachingEvent가 등록된다")
        fun `RETURNING 상태에서 접근 이벤트가 오면 RETURN 타입의 DeliveryApproachingEvent가 등록된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = saveDeliveryInReturningState()
            val robot = getAssignedRobot(delivery)
            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = pickupLocation,
                )

            eventHandler.handleApproaching(event)

            val capturedEvents = testEventListener.getCapturedEvents()
            assertThat(capturedEvents).hasSize(1)
            val approachingEvent = capturedEvents.first()
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

            eventHandler.handleApproaching(event)
        }

        @Test
        @DisplayName("로봇에 할당된 배달이 없으면 무시된다")
        fun `로봇에 할당된 배달이 없으면 접근 이벤트가 무시된다`() {
            val robot = saveRobot()
            robot.startDuty()
            robot.pullDomainEvents()
            robotRepository.saveAndFlush(robot)

            val event =
                RobotApproachingEvent(
                    robotId = robot.getRobotId(),
                    destination = Location(37.5665, 126.9780),
                )

            eventHandler.handleApproaching(event)
        }
    }

    @Nested
    @DisplayName("RobotArrivedEvent 핸들러 테스트")
    inner class HandleRobotArrivedEventTest {
        @Test
        @DisplayName("ASSIGNED 상태의 배달이 도착하면 PICKUP_ARRIVED가 된다")
        fun `ASSIGNED 상태의 배달이 도착하면 PICKUP_ARRIVED가 된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = saveDeliveryInAssignedState()
            val robot = getAssignedRobot(delivery)
            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = pickupLocation,
                )

            eventHandler.handleArrived(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.PICKUP_ARRIVED)
        }

        @Test
        @DisplayName("DELIVERING 상태의 배달이 도착하면 DELIVERY_ARRIVED가 된다")
        fun `DELIVERING 상태의 배달이 도착하면 DELIVERY_ARRIVED가 된다`() {
            val deliveryLocation = Location(latitude = 37.4979, longitude = 127.0276)
            val delivery = saveDeliveryInDeliveringState()
            val robot = getAssignedRobot(delivery)
            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = deliveryLocation,
                )

            eventHandler.handleArrived(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.DELIVERY_ARRIVED)
        }

        @Test
        @DisplayName("RETURNING 상태의 배달이 도착하면 RETURN_ARRIVED가 된다")
        fun `RETURNING 상태의 배달이 도착하면 RETURN_ARRIVED가 된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = saveDeliveryInReturningState()
            val robot = getAssignedRobot(delivery)
            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = pickupLocation,
                )

            eventHandler.handleArrived(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.RETURN_ARRIVED)
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 무시된다`() {
            val event =
                RobotArrivedEvent(
                    robotId = RobotId(99999L),
                    destination = Location(37.5665, 126.9780),
                )

            eventHandler.handleArrived(event)
        }

        @Test
        @DisplayName("로봇에 할당된 배달이 없으면 무시된다")
        fun `로봇에 할당된 배달이 없으면 무시된다`() {
            val robot = saveRobot()
            robot.startDuty()
            robot.pullDomainEvents()
            robotRepository.saveAndFlush(robot)

            val event =
                RobotArrivedEvent(
                    robotId = robot.getRobotId(),
                    destination = Location(37.5665, 126.9780),
                )

            eventHandler.handleArrived(event)
        }
    }
}
