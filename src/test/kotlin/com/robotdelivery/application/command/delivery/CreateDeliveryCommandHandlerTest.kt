package com.robotdelivery.application.command.delivery

import com.robotdelivery.domain.common.DomainEvent
import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
@DisplayName("CreateDeliveryCommandHandler 테스트")
class CreateDeliveryCommandHandlerTest {
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    private lateinit var commandHandler: CreateDeliveryCommandHandler
    private lateinit var publishedEvents: MutableList<DomainEvent>
    private lateinit var fakeEventPublisher: DomainEventPublisher

    @BeforeEach
    fun setUp() {
        deliveryRepository.deleteAll()
        publishedEvents = mutableListOf()
        fakeEventPublisher = object : DomainEventPublisher {
            override fun publishAll(events: List<DomainEvent>) {
                publishedEvents.addAll(events)
            }
        }
        commandHandler = CreateDeliveryCommandHandler(deliveryRepository, fakeEventPublisher)
    }

    private fun createCommand(
        pickupAddress: String = "서울시 중구 세종대로 110",
        pickupAddressDetail: String? = "시청역 1번 출구",
        pickupLatitude: Double = 37.5665,
        pickupLongitude: Double = 126.9780,
        deliveryAddress: String = "서울시 강남구 테헤란로 1",
        deliveryAddressDetail: String? = "2층",
        deliveryLatitude: Double = 37.4979,
        deliveryLongitude: Double = 127.0276,
        phoneNumber: String = "010-1234-5678",
    ): CreateDeliveryCommand = CreateDeliveryCommand(
        pickupAddress = pickupAddress,
        pickupAddressDetail = pickupAddressDetail,
        pickupLatitude = pickupLatitude,
        pickupLongitude = pickupLongitude,
        deliveryAddress = deliveryAddress,
        deliveryAddressDetail = deliveryAddressDetail,
        deliveryLatitude = deliveryLatitude,
        deliveryLongitude = deliveryLongitude,
        phoneNumber = phoneNumber,
    )

    @Nested
    @DisplayName("배달 생성 테스트")
    inner class CreateDeliveryTest {
        @Test
        @DisplayName("배달을 성공적으로 생성한다")
        fun `배달을 성공적으로 생성한다`() {
            val command = createCommand()

            val deliveryId = commandHandler.handle(command)

            assertNotNull(deliveryId)
            val savedDelivery = deliveryRepository.findById(deliveryId)
            assertNotNull(savedDelivery)
        }

        @Test
        @DisplayName("생성된 배달은 PENDING 상태이다")
        fun `생성된 배달은 PENDING 상태이다`() {
            val command = createCommand()

            val deliveryId = commandHandler.handle(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertEquals(DeliveryStatus.PENDING, savedDelivery.status)
        }

        @Test
        @DisplayName("픽업 목적지 정보가 올바르게 저장된다")
        fun `픽업 목적지 정보가 올바르게 저장된다`() {
            val command = createCommand(
                pickupAddress = "서울시 중구 세종대로 110",
                pickupAddressDetail = "시청역 1번 출구",
                pickupLatitude = 37.5665,
                pickupLongitude = 126.9780,
            )

            val deliveryId = commandHandler.handle(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertEquals("서울시 중구 세종대로 110", savedDelivery.pickupDestination.address)
            assertEquals("시청역 1번 출구", savedDelivery.pickupDestination.addressDetail)
            assertEquals(37.5665, savedDelivery.pickupDestination.location.latitude)
            assertEquals(126.9780, savedDelivery.pickupDestination.location.longitude)
        }

        @Test
        @DisplayName("배송 목적지 정보가 올바르게 저장된다")
        fun `배송 목적지 정보가 올바르게 저장된다`() {
            val command = createCommand(
                deliveryAddress = "서울시 강남구 테헤란로 1",
                deliveryAddressDetail = "2층",
                deliveryLatitude = 37.4979,
                deliveryLongitude = 127.0276,
            )

            val deliveryId = commandHandler.handle(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertEquals("서울시 강남구 테헤란로 1", savedDelivery.deliveryDestination.address)
            assertEquals("2층", savedDelivery.deliveryDestination.addressDetail)
            assertEquals(37.4979, savedDelivery.deliveryDestination.location.latitude)
            assertEquals(127.0276, savedDelivery.deliveryDestination.location.longitude)
        }

        @Test
        @DisplayName("전화번호가 올바르게 저장된다")
        fun `전화번호가 올바르게 저장된다`() {
            val command = createCommand(phoneNumber = "010-9999-8888")

            val deliveryId = commandHandler.handle(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertEquals("010-9999-8888", savedDelivery.phoneNumber)
        }

        @Test
        @DisplayName("주소 상세가 없어도 배달을 생성할 수 있다")
        fun `주소 상세가 없어도 배달을 생성할 수 있다`() {
            val command = createCommand(
                pickupAddressDetail = null,
                deliveryAddressDetail = null,
            )

            val deliveryId = commandHandler.handle(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertNotNull(savedDelivery)
            assertEquals(null, savedDelivery.pickupDestination.addressDetail)
            assertEquals(null, savedDelivery.deliveryDestination.addressDetail)
        }
    }

    @Nested
    @DisplayName("이벤트 발행 테스트")
    inner class EventPublishingTest {
        @Test
        @DisplayName("배달 생성 시 DeliveryCreatedEvent가 발행된다")
        fun `배달 생성 시 DeliveryCreatedEvent가 발행된다`() {
            val command = createCommand()

            commandHandler.handle(command)

            assertEquals(1, publishedEvents.size)
            val event = publishedEvents.first()
            assert(event is DeliveryCreatedEvent)
        }

        @Test
        @DisplayName("발행된 이벤트에 올바른 정보가 포함된다")
        fun `발행된 이벤트에 올바른 정보가 포함된다`() {
            val command = createCommand(
                pickupLatitude = 37.5665,
                pickupLongitude = 126.9780,
                deliveryLatitude = 37.4979,
                deliveryLongitude = 127.0276,
            )

            val deliveryId = commandHandler.handle(command)

            val event = publishedEvents.first() as DeliveryCreatedEvent
            assertEquals(deliveryId, event.deliveryId)
            assertEquals(37.5665, event.pickupLocation.latitude)
            assertEquals(126.9780, event.pickupLocation.longitude)
            assertEquals(37.4979, event.deliveryLocation.latitude)
            assertEquals(127.0276, event.deliveryLocation.longitude)
        }
    }
}

