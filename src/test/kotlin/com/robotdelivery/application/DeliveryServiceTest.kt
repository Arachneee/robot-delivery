package com.robotdelivery.application

import com.robotdelivery.domain.common.DomainEvent
import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.delivery.event.DeliveryCompletedEvent
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
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
@DisplayName("DeliveryService 테스트")
class DeliveryServiceTest {
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var robotRepository: RobotRepository

    private lateinit var deliveryService: DeliveryService
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
        deliveryService = DeliveryService(deliveryRepository, robotRepository, fakeEventPublisher)
    }

    @Nested
    @DisplayName("배달 생성 테스트")
    inner class CreateDeliveryTest {
        @Test
        @DisplayName("배달을 성공적으로 생성한다")
        fun `배달을 성공적으로 생성한다`() {
            val deliveryId =
                deliveryService.createDelivery(
                    pickupAddress = "서울시 중구 세종대로 110",
                    pickupAddressDetail = "시청역 1번 출구",
                    pickupLatitude = 37.5665,
                    pickupLongitude = 126.9780,
                    deliveryAddress = "서울시 강남구 테헤란로 1",
                    deliveryAddressDetail = "2층",
                    deliveryLatitude = 37.4979,
                    deliveryLongitude = 127.0276,
                    phoneNumber = "010-1234-5678",
                )

            assertNotNull(deliveryId)
            val savedDelivery = deliveryRepository.findById(deliveryId)
            assertNotNull(savedDelivery)
        }

        @Test
        @DisplayName("생성된 배달은 PENDING 상태이다")
        fun `생성된 배달은 PENDING 상태이다`() {
            val deliveryId =
                deliveryService.createDelivery(
                    pickupAddress = "서울시 중구 세종대로 110",
                    pickupAddressDetail = "시청역 1번 출구",
                    pickupLatitude = 37.5665,
                    pickupLongitude = 126.9780,
                    deliveryAddress = "서울시 강남구 테헤란로 1",
                    deliveryAddressDetail = "2층",
                    deliveryLatitude = 37.4979,
                    deliveryLongitude = 127.0276,
                    phoneNumber = "010-1234-5678",
                )

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertEquals(DeliveryStatus.PENDING, savedDelivery.status)
        }

        @Test
        @DisplayName("픽업 목적지 정보가 올바르게 저장된다")
        fun `픽업 목적지 정보가 올바르게 저장된다`() {
            val deliveryId =
                deliveryService.createDelivery(
                    pickupAddress = "서울시 중구 세종대로 110",
                    pickupAddressDetail = "시청역 1번 출구",
                    pickupLatitude = 37.5665,
                    pickupLongitude = 126.9780,
                    deliveryAddress = "서울시 강남구 테헤란로 1",
                    deliveryAddressDetail = "2층",
                    deliveryLatitude = 37.4979,
                    deliveryLongitude = 127.0276,
                    phoneNumber = "010-1234-5678",
                )

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertEquals("서울시 중구 세종대로 110", savedDelivery.pickupDestination.address)
            assertEquals("시청역 1번 출구", savedDelivery.pickupDestination.addressDetail)
            assertEquals(37.5665, savedDelivery.pickupDestination.location.latitude)
            assertEquals(126.9780, savedDelivery.pickupDestination.location.longitude)
        }

        @Test
        @DisplayName("배송 목적지 정보가 올바르게 저장된다")
        fun `배송 목적지 정보가 올바르게 저장된다`() {
            val deliveryId =
                deliveryService.createDelivery(
                    pickupAddress = "서울시 중구 세종대로 110",
                    pickupAddressDetail = "시청역 1번 출구",
                    pickupLatitude = 37.5665,
                    pickupLongitude = 126.9780,
                    deliveryAddress = "서울시 강남구 테헤란로 1",
                    deliveryAddressDetail = "2층",
                    deliveryLatitude = 37.4979,
                    deliveryLongitude = 127.0276,
                    phoneNumber = "010-1234-5678",
                )

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertEquals("서울시 강남구 테헤란로 1", savedDelivery.deliveryDestination.address)
            assertEquals("2층", savedDelivery.deliveryDestination.addressDetail)
            assertEquals(37.4979, savedDelivery.deliveryDestination.location.latitude)
            assertEquals(127.0276, savedDelivery.deliveryDestination.location.longitude)
        }

        @Test
        @DisplayName("전화번호가 올바르게 저장된다")
        fun `전화번호가 올바르게 저장된다`() {
            val deliveryId =
                deliveryService.createDelivery(
                    pickupAddress = "서울시 중구 세종대로 110",
                    pickupAddressDetail = null,
                    pickupLatitude = 37.5665,
                    pickupLongitude = 126.9780,
                    deliveryAddress = "서울시 강남구 테헤란로 1",
                    deliveryAddressDetail = null,
                    deliveryLatitude = 37.4979,
                    deliveryLongitude = 127.0276,
                    phoneNumber = "010-9999-8888",
                )

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertEquals("010-9999-8888", savedDelivery.phoneNumber)
        }

        @Test
        @DisplayName("주소 상세가 없어도 배달을 생성할 수 있다")
        fun `주소 상세가 없어도 배달을 생성할 수 있다`() {
            val deliveryId =
                deliveryService.createDelivery(
                    pickupAddress = "서울시 중구 세종대로 110",
                    pickupAddressDetail = null,
                    pickupLatitude = 37.5665,
                    pickupLongitude = 126.9780,
                    deliveryAddress = "서울시 강남구 테헤란로 1",
                    deliveryAddressDetail = null,
                    deliveryLatitude = 37.4979,
                    deliveryLongitude = 127.0276,
                    phoneNumber = "010-1234-5678",
                )

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertNotNull(savedDelivery)
            assertEquals(null, savedDelivery.pickupDestination.addressDetail)
            assertEquals(null, savedDelivery.deliveryDestination.addressDetail)
        }

        @Test
        @DisplayName("배달 생성 시 DeliveryCreatedEvent가 발행된다")
        fun `배달 생성 시 DeliveryCreatedEvent가 발행된다`() {
            deliveryService.createDelivery(
                pickupAddress = "서울시 중구 세종대로 110",
                pickupAddressDetail = null,
                pickupLatitude = 37.5665,
                pickupLongitude = 126.9780,
                deliveryAddress = "서울시 강남구 테헤란로 1",
                deliveryAddressDetail = null,
                deliveryLatitude = 37.4979,
                deliveryLongitude = 127.0276,
                phoneNumber = "010-1234-5678",
            )

            assertEquals(1, publishedEvents.size)
            val event = publishedEvents.first()
            assert(event is DeliveryCreatedEvent)
        }

        @Test
        @DisplayName("발행된 이벤트에 올바른 정보가 포함된다")
        fun `발행된 이벤트에 올바른 정보가 포함된다`() {
            val deliveryId =
                deliveryService.createDelivery(
                    pickupAddress = "서울시 중구 세종대로 110",
                    pickupAddressDetail = null,
                    pickupLatitude = 37.5665,
                    pickupLongitude = 126.9780,
                    deliveryAddress = "서울시 강남구 테헤란로 1",
                    deliveryAddressDetail = null,
                    deliveryLatitude = 37.4979,
                    deliveryLongitude = 127.0276,
                    phoneNumber = "010-1234-5678",
                )

            val event = publishedEvents.first() as DeliveryCreatedEvent
            assertEquals(deliveryId, event.deliveryId)
            assertEquals(37.5665, event.pickupLocation.latitude)
            assertEquals(126.9780, event.pickupLocation.longitude)
            assertEquals(37.4979, event.deliveryLocation.latitude)
            assertEquals(127.0276, event.deliveryLocation.longitude)
        }
    }

    @Nested
    @DisplayName("배달 완료 테스트")
    inner class CompleteDeliveryTest {
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
            robot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)

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

        @Test
        @DisplayName("배달을 성공적으로 완료한다")
        fun `배달을 성공적으로 완료한다`() {
            val (delivery, robot) = setupDeliveryInDroppingOffState()

            deliveryService.completeDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertEquals(DeliveryStatus.COMPLETED, updatedDelivery.status)
            assertNotNull(updatedDelivery.completedAt)
        }

        @Test
        @DisplayName("배달 완료 시 로봇이 READY 상태가 된다")
        fun `배달 완료 시 로봇이 READY 상태가 된다`() {
            val (delivery, robot) = setupDeliveryInDroppingOffState()

            deliveryService.completeDelivery(delivery.getDeliveryId())

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertEquals(RobotStatus.READY, updatedRobot.status)
            assertNull(updatedRobot.currentDeliveryId)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 완료 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 완료 시 예외가 발생한다`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    deliveryService.completeDelivery(
                        com.robotdelivery.domain.common.DeliveryId(99999L),
                    )
                }

            assertEquals("배달을 찾을 수 없습니다: 99999", exception.message)
        }

        @Test
        @DisplayName("배차되지 않은 배달 완료 시 예외가 발생한다")
        fun `배차되지 않은 배달 완료 시 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.pullDomainEvents() // 생성 이벤트 클리어 (@PostPersist 이후)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.completeDelivery(delivery.getDeliveryId())
                }

            assertEquals("배차된 로봇이 없습니다.", exception.message)
        }

        @Test
        @DisplayName("배달 완료 시 DeliveryCompletedEvent가 발행된다")
        fun `배달 완료 시 DeliveryCompletedEvent가 발행된다`() {
            val (delivery, robot) = setupDeliveryInDroppingOffState()

            deliveryService.completeDelivery(delivery.getDeliveryId())

            val completedEvent = publishedEvents.filterIsInstance<DeliveryCompletedEvent>().firstOrNull()
            assertNotNull(completedEvent)
            assertEquals(delivery.getDeliveryId(), completedEvent!!.deliveryId)
            assertEquals(robot.getRobotId(), completedEvent.robotId)
        }

        @Test
        @DisplayName("배달 완료 시 RobotBecameAvailableEvent가 발행된다")
        fun `배달 완료 시 RobotBecameAvailableEvent가 발행된다`() {
            val (delivery, robot) = setupDeliveryInDroppingOffState()

            deliveryService.completeDelivery(delivery.getDeliveryId())

            val availableEvent = publishedEvents.filterIsInstance<RobotBecameAvailableEvent>().firstOrNull()
            assertNotNull(availableEvent)
            assertEquals(robot.getRobotId(), availableEvent!!.robotId)
        }
    }
}

