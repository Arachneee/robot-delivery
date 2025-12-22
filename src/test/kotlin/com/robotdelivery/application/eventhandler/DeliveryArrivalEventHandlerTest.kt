package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.domain.robot.event.RobotArrivedAtDestinationEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("DeliveryArrivalEventHandler 테스트")
class DeliveryArrivalEventHandlerTest {
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var robotRepository: RobotRepository

    private lateinit var eventHandler: DeliveryArrivalEventHandler

    @BeforeEach
    fun setUp() {
        deliveryRepository.deleteAll()
        robotRepository.deleteAll()
        eventHandler = DeliveryArrivalEventHandler(deliveryRepository, robotRepository)
    }

    private fun createDelivery(pickupLocation: Location = Location(latitude = 37.5665, longitude = 126.9780)): Delivery {
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
        val savedDelivery = deliveryRepository.save(delivery)
        savedDelivery.pullDomainEvents() // 생성 이벤트 클리어 (@PostPersist 이후)
        return savedDelivery
    }

    private fun createRobotWithDelivery(
        name: String = "로봇-1",
        location: Location = Location(latitude = 37.5665, longitude = 126.9780),
        delivery: Delivery,
    ): Robot {
        val robot =
            Robot(
                name = name,
                status = RobotStatus.OFF_DUTY,
                battery = 100,
                location = location,
            )
        val savedRobot = robotRepository.save(robot)
        savedRobot.startDuty()
        savedRobot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)
        savedRobot.pullDomainEvents()
        return robotRepository.save(savedRobot)
    }

    @Nested
    @DisplayName("RobotArrivedAtDestinationEvent 핸들러 테스트")
    inner class HandleRobotArrivedAtDestinationEventTest {
        @Test
        @DisplayName("ASSIGNED 상태의 배달이 도착하면 PICKUP_ARRIVED가 된다")
        fun `ASSIGNED 상태의 배달이 도착하면 PICKUP_ARRIVED가 된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation)
            delivery.assignRobot(RobotId(1L))
            deliveryRepository.save(delivery)

            val robot = createRobotWithDelivery(delivery = delivery)
            val event = RobotArrivedAtDestinationEvent(
                robotId = robot.getRobotId(),
                destination = pickupLocation,
            )

            eventHandler.handle(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.PICKUP_ARRIVED, updatedDelivery.status)
        }

        @Test
        @DisplayName("DELIVERING 상태의 배달이 도착하면 DELIVERY_ARRIVED가 된다")
        fun `DELIVERING 상태의 배달이 도착하면 DELIVERY_ARRIVED가 된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val deliveryLocation = Location(latitude = 37.4979, longitude = 127.0276)
            val delivery = createDelivery(pickupLocation)
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()   // PICKUP_ARRIVED
            delivery.openDoor()  // PICKING_UP
            delivery.startDelivery() // DELIVERING
            deliveryRepository.save(delivery)

            val robot = createRobotWithDelivery(delivery = delivery, location = deliveryLocation)
            val event = RobotArrivedAtDestinationEvent(
                robotId = robot.getRobotId(),
                destination = deliveryLocation,
            )

            eventHandler.handle(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.DELIVERY_ARRIVED, updatedDelivery.status)
        }

        @Test
        @DisplayName("RETURNING 상태의 배달이 도착하면 RETURN_ARRIVED가 된다")
        fun `RETURNING 상태의 배달이 도착하면 RETURN_ARRIVED가 된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation)
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()   // PICKUP_ARRIVED
            delivery.openDoor()  // PICKING_UP
            delivery.startDelivery() // DELIVERING
            delivery.cancel()    // RETURNING
            deliveryRepository.save(delivery)

            val robot = createRobotWithDelivery(delivery = delivery)
            val event = RobotArrivedAtDestinationEvent(
                robotId = robot.getRobotId(),
                destination = pickupLocation,
            )

            eventHandler.handle(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.RETURN_ARRIVED, updatedDelivery.status)
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 무시된다`() {
            val event = RobotArrivedAtDestinationEvent(
                robotId = RobotId(99999L),
                destination = Location(37.5665, 126.9780),
            )

            // 예외 없이 정상 종료
            eventHandler.handle(event)
        }

        @Test
        @DisplayName("로봇에 할당된 배달이 없으면 무시된다")
        fun `로봇에 할당된 배달이 없으면 무시된다`() {
            val robot = Robot(
                name = "로봇-1",
                status = RobotStatus.OFF_DUTY,
                battery = 100,
                location = Location(37.5665, 126.9780),
            )
            val savedRobot = robotRepository.save(robot)
            savedRobot.startDuty()
            savedRobot.pullDomainEvents()
            robotRepository.save(savedRobot)

            val event = RobotArrivedAtDestinationEvent(
                robotId = savedRobot.getRobotId(),
                destination = Location(37.5665, 126.9780),
            )

            // 예외 없이 정상 종료
            eventHandler.handle(event)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID가 할당된 경우 무시된다")
        fun `존재하지 않는 배달 ID가 할당된 경우 무시된다`() {
            val delivery = createDelivery()
            val robot = createRobotWithDelivery(delivery = delivery)

            // 배달 삭제
            deliveryRepository.delete(delivery)

            val event = RobotArrivedAtDestinationEvent(
                robotId = robot.getRobotId(),
                destination = Location(37.5665, 126.9780),
            )

            // 예외 없이 정상 종료
            eventHandler.handle(event)
        }
    }
}

