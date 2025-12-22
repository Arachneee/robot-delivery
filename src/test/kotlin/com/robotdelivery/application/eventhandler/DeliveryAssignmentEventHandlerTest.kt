package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.DomainEvent
import com.robotdelivery.domain.common.DomainEventPublisher
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("DeliveryAssignmentEventHandler 테스트")
class DeliveryAssignmentEventHandlerTest {
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var robotRepository: RobotRepository

    private lateinit var eventHandler: DeliveryAssignmentEventHandler
    private lateinit var assignmentService: DeliveryAssignmentService
    private lateinit var publishedEvents: MutableList<DomainEvent>
    private lateinit var fakeEventPublisher: DomainEventPublisher

    @BeforeEach
    fun setUp() {
        deliveryRepository.deleteAll()
        robotRepository.deleteAll()
        publishedEvents = mutableListOf()
        fakeEventPublisher =
            object : DomainEventPublisher {
                override fun publishAll(events: List<DomainEvent>) {
                    publishedEvents.addAll(events)
                }
            }
        assignmentService = DeliveryAssignmentService(robotRepository, deliveryRepository)
        eventHandler =
            DeliveryAssignmentEventHandler(
                deliveryRepository,
                robotRepository,
                assignmentService,
                fakeEventPublisher,
            )
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

    private fun createAvailableRobot(
        name: String = "로봇-1",
        location: Location = Location(latitude = 37.5665, longitude = 126.9780),
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
        savedRobot.pullDomainEvents() // 이벤트 클리어
        return robotRepository.save(savedRobot)
    }

    @Nested
    @DisplayName("DeliveryCreatedEvent 핸들러 테스트")
    inner class HandleDeliveryCreatedEventTest {
        @Test
        @DisplayName("배달이 생성되면 가장 가까운 로봇이 배정된다")
        fun `배달이 생성되면 가장 가까운 로봇이 배정된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation)
            val robot = createAvailableRobot(location = Location(37.5660, 126.9770))
            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = pickupLocation,
                    deliveryLocation = Location(37.4979, 127.0276),
                )

            eventHandler.dispatch(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.ASSIGNED, updatedDelivery.status)
            assertEquals(robot.getRobotId(), updatedDelivery.assignedRobotId)
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

            eventHandler.dispatch(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.PENDING, updatedDelivery.status)
            assertNull(updatedDelivery.assignedRobotId)
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

            // 예외 없이 정상 종료
            eventHandler.dispatch(event)
        }

        @Test
        @DisplayName("여러 로봇 중 가장 가까운 로봇이 선택된다")
        fun `여러 로봇 중 가장 가까운 로봇이 선택된다`() {
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val delivery = createDelivery(pickupLocation)

            // 거리 순서: nearRobot < farRobot
            val farRobot = createAvailableRobot(name = "로봇-far", location = Location(37.5020, 127.0020))
            val nearRobot = createAvailableRobot(name = "로봇-near", location = Location(37.5005, 127.0005))

            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = pickupLocation,
                    deliveryLocation = delivery.deliveryDestination.location,
                )

            eventHandler.dispatch(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(nearRobot.getRobotId(), updatedDelivery.assignedRobotId)
        }
    }

    @Nested
    @DisplayName("RobotBecameAvailableEvent 핸들러 테스트")
    inner class HandleRobotBecameAvailableEventTest {
        @Test
        @DisplayName("로봇이 가용해지면 대기 중인 가장 가까운 배달이 배정된다")
        fun `로봇이 가용해지면 대기 중인 가장 가까운 배달이 배정된다`() {
            val robotLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val robot = createAvailableRobot(location = robotLocation)
            val delivery = createDelivery(pickupLocation = Location(37.5660, 126.9770))

            val event =
                RobotBecameAvailableEvent(
                    robotId = robot.getRobotId(),
                    location = robotLocation,
                )

            eventHandler.dispatch(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.ASSIGNED, updatedDelivery.status)
            assertEquals(robot.getRobotId(), updatedDelivery.assignedRobotId)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertEquals(RobotStatus.BUSY, updatedRobot.status)
            assertEquals(delivery.getDeliveryId(), updatedRobot.currentDeliveryId)
        }

        @Test
        @DisplayName("대기 중인 배달이 없으면 배정되지 않는다")
        fun `대기 중인 배달이 없으면 배정되지 않는다`() {
            val robot = createAvailableRobot()
            val event =
                RobotBecameAvailableEvent(
                    robotId = robot.getRobotId(),
                    location = robot.location,
                )

            eventHandler.dispatch(event)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertEquals(RobotStatus.READY, updatedRobot.status)
            assertNull(updatedRobot.currentDeliveryId)
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 무시된다`() {
            val event =
                RobotBecameAvailableEvent(
                    robotId = RobotId(99999L),
                    location = Location(37.5665, 126.9780),
                )

            // 예외 없이 정상 종료
            eventHandler.dispatch(event)
        }

        @Test
        @DisplayName("로봇이 가용 상태가 아니면 배정되지 않는다")
        fun `로봇이 가용 상태가 아니면 배정되지 않는다`() {
            val delivery = createDelivery()
            val robot =
                Robot(
                    name = "로봇-1",
                    status = RobotStatus.OFF_DUTY,
                    battery = 100,
                    location = Location(37.5665, 126.9780),
                )
            val savedRobot = robotRepository.save(robot)

            val event =
                RobotBecameAvailableEvent(
                    robotId = savedRobot.getRobotId(),
                    location = savedRobot.location,
                )

            eventHandler.dispatch(event)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.PENDING, updatedDelivery.status)
        }

        @Test
        @DisplayName("여러 배달 중 가장 가까운 배달이 선택된다")
        fun `여러 배달 중 가장 가까운 배달이 선택된다`() {
            val robotLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val robot = createAvailableRobot(location = robotLocation)

            // 거리 순서: nearDelivery < farDelivery
            val farDelivery = createDelivery(pickupLocation = Location(37.5020, 127.0020))
            val nearDelivery = createDelivery(pickupLocation = Location(37.5005, 127.0005))

            val event =
                RobotBecameAvailableEvent(
                    robotId = robot.getRobotId(),
                    location = robotLocation,
                )

            eventHandler.dispatch(event)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertEquals(nearDelivery.getDeliveryId(), updatedRobot.currentDeliveryId)
        }
    }

    @Nested
    @DisplayName("이벤트 발행 테스트")
    inner class EventPublishingTest {
        @Test
        @DisplayName("배정 성공 시 도메인 이벤트가 발행된다")
        fun `배정 성공 시 도메인 이벤트가 발행된다`() {
            val delivery = createDelivery()
            val robot = createAvailableRobot()
            val event =
                DeliveryCreatedEvent(
                    deliveryId = delivery.getDeliveryId(),
                    pickupLocation = delivery.pickupDestination.location,
                    deliveryLocation = delivery.deliveryDestination.location,
                )

            eventHandler.dispatch(event)

            // 배달과 로봇의 도메인 이벤트가 발행됨
            assert(publishedEvents.isNotEmpty())
        }
    }
}
