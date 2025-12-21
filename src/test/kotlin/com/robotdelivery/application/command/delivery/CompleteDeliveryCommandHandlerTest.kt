package com.robotdelivery.application.command.delivery

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.DomainEvent
import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.delivery.event.DeliveryCompletedEvent
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("CompleteDeliveryCommandHandler 테스트")
class CompleteDeliveryCommandHandlerTest {
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var robotRepository: RobotRepository

    private lateinit var commandHandler: CompleteDeliveryCommandHandler
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
        commandHandler = CompleteDeliveryCommandHandler(deliveryRepository, robotRepository, fakeEventPublisher)
    }

    private fun createDelivery(): Delivery =
        deliveryRepository.save(
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
            ),
        )

    private fun createRobot(name: String = "로봇-1"): Robot =
        robotRepository.save(
            Robot(
                name = name,
                status = RobotStatus.OFF_DUTY,
                battery = 100,
                location = Location(latitude = 37.5665, longitude = 126.9780),
            ),
        )

    private fun setupDeliveryInDroppingOffState(): Pair<Delivery, Robot> {
        val delivery = createDelivery()
        val robot = createRobot()

        // 로봇 출근
        robot.startDuty()
        robot.pullDomainEvents() // 이벤트 클리어

        // 배달에 로봇 배정
        delivery.assignRobot(robot.getRobotId())
        robot.assignDelivery(delivery.getDeliveryId())

        // 픽업 완료까지 상태 전이
        delivery.arrived() // ASSIGNED -> PICKUP_ARRIVED
        delivery.openDoor() // PICKUP_ARRIVED -> PICKING_UP
        delivery.startDelivery() // PICKING_UP -> DELIVERING
        delivery.arrived() // DELIVERING -> DELIVERY_ARRIVED
        delivery.openDoor() // DELIVERY_ARRIVED -> DROPPING_OFF

        delivery.pullDomainEvents() // 이벤트 클리어
        robot.pullDomainEvents() // 이벤트 클리어

        return Pair(deliveryRepository.save(delivery), robotRepository.save(robot))
    }

    @Nested
    @DisplayName("배달 완료 테스트")
    inner class CompleteDeliveryTest {
        @Test
        @DisplayName("배달을 성공적으로 완료한다")
        fun `배달을 성공적으로 완료한다`() {
            val (delivery, robot) = setupDeliveryInDroppingOffState()
            val command = CompleteDeliveryCommand(delivery.getDeliveryId())

            commandHandler.handle(command)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.COMPLETED, updatedDelivery.status)
            assertNotNull(updatedDelivery.completedAt)
        }

        @Test
        @DisplayName("배달 완료 시 로봇이 READY 상태가 된다")
        fun `배달 완료 시 로봇이 READY 상태가 된다`() {
            val (delivery, robot) = setupDeliveryInDroppingOffState()
            val command = CompleteDeliveryCommand(delivery.getDeliveryId())

            commandHandler.handle(command)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertEquals(RobotStatus.READY, updatedRobot.status)
            assertNull(updatedRobot.currentDeliveryId)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 완료 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 완료 시 예외가 발생한다`() {
            val command = CompleteDeliveryCommand(DeliveryId(99999L))

            val exception =
                assertThrows<IllegalArgumentException> {
                    commandHandler.handle(command)
                }

            assertEquals("배달을 찾을 수 없습니다: 99999", exception.message)
        }

        @Test
        @DisplayName("배차되지 않은 배달 완료 시 예외가 발생한다")
        fun `배차되지 않은 배달 완료 시 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.pullDomainEvents() // 생성 이벤트 클리어 (@PostPersist 이후)

            val command = CompleteDeliveryCommand(delivery.getDeliveryId())

            val exception =
                assertThrows<IllegalStateException> {
                    commandHandler.handle(command)
                }

            assertEquals("배차된 로봇이 없습니다.", exception.message)
        }
    }

    @Nested
    @DisplayName("이벤트 발행 테스트")
    inner class EventPublishingTest {
        @Test
        @DisplayName("배달 완료 시 DeliveryCompletedEvent가 발행된다")
        fun `배달 완료 시 DeliveryCompletedEvent가 발행된다`() {
            val (delivery, robot) = setupDeliveryInDroppingOffState()
            val command = CompleteDeliveryCommand(delivery.getDeliveryId())

            commandHandler.handle(command)

            val completedEvent = publishedEvents.filterIsInstance<DeliveryCompletedEvent>().firstOrNull()
            assertNotNull(completedEvent)
            assertEquals(delivery.getDeliveryId(), completedEvent!!.deliveryId)
            assertEquals(robot.getRobotId(), completedEvent.robotId)
        }

        @Test
        @DisplayName("배달 완료 시 RobotBecameAvailableEvent가 발행된다")
        fun `배달 완료 시 RobotBecameAvailableEvent가 발행된다`() {
            val (delivery, robot) = setupDeliveryInDroppingOffState()
            val command = CompleteDeliveryCommand(delivery.getDeliveryId())

            commandHandler.handle(command)

            val availableEvent = publishedEvents.filterIsInstance<RobotBecameAvailableEvent>().firstOrNull()
            assertNotNull(availableEvent)
            assertEquals(robot.getRobotId(), availableEvent!!.robotId)
        }
    }
}
