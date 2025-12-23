package com.robotdelivery.application

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("DeliveryService 테스트")
class DeliveryServiceTest {
    @Mock
    private lateinit var deliveryRepository: DeliveryRepository

    @Mock
    private lateinit var robotRepository: RobotRepository

    @Mock
    private lateinit var robotClient: RobotClient

    private lateinit var deliveryService: DeliveryService

    @BeforeEach
    fun setUp() {
        deliveryService = DeliveryService(deliveryRepository, robotRepository, robotClient)
    }

    @Nested
    @DisplayName("배달 생성 테스트")
    inner class CreateDeliveryTest {
        @Test
        @DisplayName("배달을 성공적으로 생성한다")
        fun `배달을 성공적으로 생성한다`() {
            val savedDelivery = createDelivery(1L)
            whenever(deliveryRepository.saveAndFlush(any<Delivery>())).thenReturn(savedDelivery)

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
            assertEquals(DeliveryId(1L), deliveryId)
        }

        @Test
        @DisplayName("생성된 배달은 PENDING 상태이다")
        fun `생성된 배달은 PENDING 상태이다`() {
            val deliveryCaptor = argumentCaptor<Delivery>()
            val savedDelivery = createDelivery(1L)
            whenever(deliveryRepository.saveAndFlush(any<Delivery>())).thenReturn(savedDelivery)

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

            verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture())
            assertEquals(DeliveryStatus.PENDING, deliveryCaptor.firstValue.status)
        }

        @Test
        @DisplayName("픽업 목적지 정보가 올바르게 저장된다")
        fun `픽업 목적지 정보가 올바르게 저장된다`() {
            val deliveryCaptor = argumentCaptor<Delivery>()
            val savedDelivery = createDelivery(1L)
            whenever(deliveryRepository.saveAndFlush(any<Delivery>())).thenReturn(savedDelivery)

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

            verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture())
            val capturedDelivery = deliveryCaptor.firstValue
            assertEquals("서울시 중구 세종대로 110", capturedDelivery.pickupDestination.address)
            assertEquals("시청역 1번 출구", capturedDelivery.pickupDestination.addressDetail)
            assertEquals(37.5665, capturedDelivery.pickupDestination.location.latitude)
            assertEquals(126.9780, capturedDelivery.pickupDestination.location.longitude)
        }

        @Test
        @DisplayName("배송 목적지 정보가 올바르게 저장된다")
        fun `배송 목적지 정보가 올바르게 저장된다`() {
            val deliveryCaptor = argumentCaptor<Delivery>()
            val savedDelivery = createDelivery(1L)
            whenever(deliveryRepository.saveAndFlush(any<Delivery>())).thenReturn(savedDelivery)

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

            verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture())
            val capturedDelivery = deliveryCaptor.firstValue
            assertEquals("서울시 강남구 테헤란로 1", capturedDelivery.deliveryDestination.address)
            assertEquals("2층", capturedDelivery.deliveryDestination.addressDetail)
            assertEquals(37.4979, capturedDelivery.deliveryDestination.location.latitude)
            assertEquals(127.0276, capturedDelivery.deliveryDestination.location.longitude)
        }

        @Test
        @DisplayName("전화번호가 올바르게 저장된다")
        fun `전화번호가 올바르게 저장된다`() {
            val deliveryCaptor = argumentCaptor<Delivery>()
            val savedDelivery = createDelivery(1L)
            whenever(deliveryRepository.saveAndFlush(any<Delivery>())).thenReturn(savedDelivery)

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

            verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture())
            assertEquals("010-9999-8888", deliveryCaptor.firstValue.phoneNumber)
        }

        @Test
        @DisplayName("주소 상세가 없어도 배달을 생성할 수 있다")
        fun `주소 상세가 없어도 배달을 생성할 수 있다`() {
            val deliveryCaptor = argumentCaptor<Delivery>()
            val savedDelivery = createDelivery(1L)
            whenever(deliveryRepository.saveAndFlush(any<Delivery>())).thenReturn(savedDelivery)

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

            verify(deliveryRepository).saveAndFlush(deliveryCaptor.capture())
            val capturedDelivery = deliveryCaptor.firstValue
            assertNull(capturedDelivery.pickupDestination.addressDetail)
            assertNull(capturedDelivery.deliveryDestination.addressDetail)
        }
    }

    @Nested
    @DisplayName("배달 완료 테스트")
    inner class CompleteDeliveryTest {
        @Test
        @DisplayName("배달을 성공적으로 완료한다")
        fun `배달을 성공적으로 완료한다`() {
            val delivery = createDeliveryInDroppingOffState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.completeDelivery(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.COMPLETED, delivery.status)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("배달 완료 시 로봇이 READY 상태가 된다")
        fun `배달 완료 시 로봇이 READY 상태가 된다`() {
            val delivery = createDeliveryInDroppingOffState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.completeDelivery(delivery.getDeliveryId())

            assertEquals(RobotStatus.READY, robot.status)
            assertNull(robot.currentDeliveryId)
            verify(robotRepository).save(robot)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 완료 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 완료 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)
            whenever(deliveryRepository.findById(deliveryId)).thenReturn(null)

            val exception =
                assertThrows<IllegalArgumentException> {
                    deliveryService.completeDelivery(deliveryId)
                }

            assertEquals("배달을 찾을 수 없습니다: 99999", exception.message)
        }

        @Test
        @DisplayName("배차되지 않은 배달 완료 시 예외가 발생한다")
        fun `배차되지 않은 배달 완료 시 예외가 발생한다`() {
            val delivery = createDelivery(1L)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.completeDelivery(delivery.getDeliveryId())
                }

            assertEquals("배차된 로봇이 없습니다.", exception.message)
        }
    }

    @Nested
    @DisplayName("문열기 테스트")
    inner class OpenDoorTest {
        @Test
        @DisplayName("픽업 도착 상태에서 문을 열면 PICKING_UP 상태가 된다")
        fun `픽업 도착 상태에서 문을 열면 PICKING_UP 상태가 된다`() {
            val delivery = createDeliveryInPickupArrivedState()
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            deliveryService.openDoor(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.PICKING_UP, delivery.status)
            verify(deliveryRepository).save(delivery)
            verify(robotClient).openDoor(delivery.assignedRobotId!!)
        }

        @Test
        @DisplayName("배송 도착 상태에서 문을 열면 DROPPING_OFF 상태가 된다")
        fun `배송 도착 상태에서 문을 열면 DROPPING_OFF 상태가 된다`() {
            val delivery = createDeliveryInDeliveryArrivedState()
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            deliveryService.openDoor(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.DROPPING_OFF, delivery.status)
            verify(deliveryRepository).save(delivery)
            verify(robotClient).openDoor(delivery.assignedRobotId!!)
        }

        @Test
        @DisplayName("문열기 시 RobotClient.openDoor가 호출된다")
        fun `문열기 시 RobotClient openDoor가 호출된다`() {
            val delivery = createDeliveryInPickupArrivedState()
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            deliveryService.openDoor(delivery.getDeliveryId())

            verify(robotClient).openDoor(delivery.assignedRobotId!!)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 문열기 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 문열기 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)
            whenever(deliveryRepository.findById(deliveryId)).thenReturn(null)

            val exception =
                assertThrows<IllegalArgumentException> {
                    deliveryService.openDoor(deliveryId)
                }

            assertEquals("배달을 찾을 수 없습니다: 99999", exception.message)
        }

        @Test
        @DisplayName("배차되지 않은 배달의 문열기 시 예외가 발생한다")
        fun `배차되지 않은 배달의 문열기 시 예외가 발생한다`() {
            val delivery = createDelivery(1L)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.openDoor(delivery.getDeliveryId())
                }

            assertEquals("배차된 로봇이 없습니다.", exception.message)
        }

        @Test
        @DisplayName("문을 열 수 없는 상태에서 문열기 시 예외가 발생한다")
        fun `문을 열 수 없는 상태에서 문열기 시 예외가 발생한다`() {
            val delivery = createDeliveryInAssignedState()
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.openDoor(delivery.getDeliveryId())
                }

            assertTrue(exception.message!!.contains("문을 열 수 없는 상태입니다"))
        }
    }

    @Nested
    @DisplayName("배송 시작 테스트")
    inner class StartDeliveryTest {
        @Test
        @DisplayName("배송을 성공적으로 시작한다")
        fun `배송을 성공적으로 시작한다`() {
            val delivery = createDeliveryInPickingUpState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.startDelivery(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.DELIVERING, delivery.status)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("배송 시작 시 로봇에게 배송 목적지로 이동 명령이 전달된다")
        fun `배송 시작 시 로봇에게 배송 목적지로 이동 명령이 전달된다`() {
            val delivery = createDeliveryInPickingUpState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.startDelivery(delivery.getDeliveryId())

            assertEquals(delivery.deliveryDestination.location, robot.destination)
            verify(robotRepository).save(robot)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 배송 시작 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 배송 시작 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)
            whenever(deliveryRepository.findById(deliveryId)).thenReturn(null)

            val exception =
                assertThrows<IllegalArgumentException> {
                    deliveryService.startDelivery(deliveryId)
                }

            assertEquals("배달을 찾을 수 없습니다: 99999", exception.message)
        }

        @Test
        @DisplayName("배차되지 않은 배달의 배송 시작 시 예외가 발생한다")
        fun `배차되지 않은 배달의 배송 시작 시 예외가 발생한다`() {
            val delivery = createDelivery(1L)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.startDelivery(delivery.getDeliveryId())
                }

            assertEquals("배차된 로봇이 없습니다.", exception.message)
        }

        @Test
        @DisplayName("픽업 중 상태가 아닌 배달의 배송 시작 시 예외가 발생한다")
        fun `픽업 중 상태가 아닌 배달의 배송 시작 시 예외가 발생한다`() {
            val delivery = createDeliveryInAssignedState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.startDelivery(delivery.getDeliveryId())
                }

            assertTrue(exception.message!!.contains("픽업 중 상태에서만 배송을 시작할 수 있습니다"))
        }
    }

    @Nested
    @DisplayName("회수 완료 테스트")
    inner class CompleteReturnTest {
        @Test
        @DisplayName("회수를 성공적으로 완료한다")
        fun `회수를 성공적으로 완료한다`() {
            val delivery = createDeliveryInReturningOffState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.completeReturn(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.RETURN_COMPLETED, delivery.status)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("회수 완료 시 로봇이 READY 상태가 된다")
        fun `회수 완료 시 로봇이 READY 상태가 된다`() {
            val delivery = createDeliveryInReturningOffState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.completeReturn(delivery.getDeliveryId())

            assertEquals(RobotStatus.READY, robot.status)
            assertNull(robot.currentDeliveryId)
            verify(robotRepository).save(robot)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 회수 완료 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 회수 완료 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)
            whenever(deliveryRepository.findById(deliveryId)).thenReturn(null)

            val exception =
                assertThrows<IllegalArgumentException> {
                    deliveryService.completeReturn(deliveryId)
                }

            assertEquals("배달을 찾을 수 없습니다: 99999", exception.message)
        }

        @Test
        @DisplayName("배차되지 않은 배달 회수 완료 시 예외가 발생한다")
        fun `배차되지 않은 배달 회수 완료 시 예외가 발생한다`() {
            val delivery = createDelivery(1L)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.completeReturn(delivery.getDeliveryId())
                }

            assertEquals("배차된 로봇이 없습니다.", exception.message)
        }

        @Test
        @DisplayName("회수 중 상태가 아닌 배달의 회수 완료 시 예외가 발생한다")
        fun `회수 중 상태가 아닌 배달의 회수 완료 시 예외가 발생한다`() {
            val delivery = createDeliveryInAssignedState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.completeReturn(delivery.getDeliveryId())
                }

            assertTrue(exception.message!!.contains("회수 완료 처리할 수 없는 상태입니다"))
        }
    }

    @Nested
    @DisplayName("배달 취소 테스트")
    inner class CancelDeliveryTest {
        @Test
        @DisplayName("PENDING 상태의 배달을 취소한다")
        fun `PENDING 상태의 배달을 취소한다`() {
            val delivery = createDelivery(1L)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.CANCELED, delivery.status)
            assertEquals(false, requiresReturn)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("ASSIGNED 상태의 배달을 취소하면 로봇이 해제된다")
        fun `ASSIGNED 상태의 배달을 취소하면 로봇이 해제된다`() {
            val delivery = createDeliveryInAssignedState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.CANCELED, delivery.status)
            assertEquals(false, requiresReturn)
            assertEquals(RobotStatus.READY, robot.status)
            assertNull(robot.currentDeliveryId)
            verify(robotRepository).save(robot)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태의 배달을 취소하면 로봇이 해제된다")
        fun `PICKUP_ARRIVED 상태의 배달을 취소하면 로봇이 해제된다`() {
            val delivery = createDeliveryInPickupArrivedState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.CANCELED, delivery.status)
            assertEquals(false, requiresReturn)
            assertEquals(RobotStatus.READY, robot.status)
            assertNull(robot.currentDeliveryId)
            verify(robotRepository).save(robot)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("PICKING_UP 상태의 배달을 취소하면 회수가 시작된다")
        fun `PICKING_UP 상태의 배달을 취소하면 회수가 시작된다`() {
            val delivery = createDeliveryInPickingUpState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.RETURNING, delivery.status)
            assertEquals(true, requiresReturn)
            assertEquals(delivery.pickupDestination.location, robot.destination)
            verify(robotRepository).save(robot)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("DELIVERING 상태의 배달을 취소하면 회수가 시작된다")
        fun `DELIVERING 상태의 배달을 취소하면 회수가 시작된다`() {
            val delivery = createDeliveryInDeliveringState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.RETURNING, delivery.status)
            assertEquals(true, requiresReturn)
            assertEquals(delivery.pickupDestination.location, robot.destination)
            verify(robotRepository).save(robot)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("DROPPING_OFF 상태의 배달을 취소하면 회수가 시작된다")
        fun `DROPPING_OFF 상태의 배달을 취소하면 회수가 시작된다`() {
            val delivery = createDeliveryInDroppingOffState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.RETURNING, delivery.status)
            assertEquals(true, requiresReturn)
            assertEquals(delivery.pickupDestination.location, robot.destination)
            verify(robotRepository).save(robot)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 취소 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 취소 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)
            whenever(deliveryRepository.findById(deliveryId)).thenReturn(null)

            val exception =
                assertThrows<IllegalArgumentException> {
                    deliveryService.cancelDelivery(deliveryId)
                }

            assertEquals("배달을 찾을 수 없습니다: 99999", exception.message)
        }

        @Test
        @DisplayName("이미 완료된 배달은 취소할 수 없다")
        fun `이미 완료된 배달은 취소할 수 없다`() {
            val delivery = createDeliveryInCompletedState()
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.cancelDelivery(delivery.getDeliveryId())
                }

            assertTrue(exception.message!!.contains("취소할 수 없는 상태입니다"))
        }

        @Test
        @DisplayName("이미 취소된 배달은 다시 취소할 수 없다")
        fun `이미 취소된 배달은 다시 취소할 수 없다`() {
            val delivery = createDeliveryInCanceledState()
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.cancelDelivery(delivery.getDeliveryId())
                }

            assertTrue(exception.message!!.contains("취소할 수 없는 상태입니다"))
        }
    }

    @Nested
    @DisplayName("배차 취소 테스트")
    inner class UnassignRobotTest {
        @Test
        @DisplayName("배차를 성공적으로 취소한다")
        fun `배차를 성공적으로 취소한다`() {
            val delivery = createDeliveryInAssignedState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.unassignRobot(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.PENDING, delivery.status)
            assertNull(delivery.assignedRobotId)
            verify(deliveryRepository).save(delivery)
        }

        @Test
        @DisplayName("배차 취소 시 로봇이 READY 상태가 된다")
        fun `배차 취소 시 로봇이 READY 상태가 된다`() {
            val delivery = createDeliveryInAssignedState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.unassignRobot(delivery.getDeliveryId())

            assertEquals(RobotStatus.READY, robot.status)
            assertNull(robot.currentDeliveryId)
            verify(robotRepository).save(robot)
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태에서 배차를 취소할 수 있다")
        fun `PICKUP_ARRIVED 상태에서 배차를 취소할 수 있다`() {
            val delivery = createDeliveryInPickupArrivedState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.unassignRobot(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.PENDING, delivery.status)
            assertEquals(RobotStatus.READY, robot.status)
        }

        @Test
        @DisplayName("PICKING_UP 상태에서 배차를 취소할 수 있다")
        fun `PICKING_UP 상태에서 배차를 취소할 수 있다`() {
            val delivery = createDeliveryInPickingUpState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            deliveryService.unassignRobot(delivery.getDeliveryId())

            assertEquals(DeliveryStatus.PENDING, delivery.status)
            assertEquals(RobotStatus.READY, robot.status)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 배차 취소 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 배차 취소 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)
            whenever(deliveryRepository.findById(deliveryId)).thenReturn(null)

            val exception =
                assertThrows<IllegalArgumentException> {
                    deliveryService.unassignRobot(deliveryId)
                }

            assertEquals("배달을 찾을 수 없습니다: 99999", exception.message)
        }

        @Test
        @DisplayName("배차되지 않은 배달의 배차 취소 시 예외가 발생한다")
        fun `배차되지 않은 배달의 배차 취소 시 예외가 발생한다`() {
            val delivery = createDelivery(1L)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.unassignRobot(delivery.getDeliveryId())
                }

            assertEquals("배차된 로봇이 없습니다.", exception.message)
        }

        @Test
        @DisplayName("배달 출발 후에는 배차 취소할 수 없다")
        fun `배달 출발 후에는 배차 취소할 수 없다`() {
            val delivery = createDeliveryInDeliveringState()
            val robot = createRobotWithDelivery(delivery)
            whenever(deliveryRepository.findById(delivery.getDeliveryId())).thenReturn(delivery)
            whenever(robotRepository.findById(delivery.assignedRobotId!!)).thenReturn(robot)

            val exception =
                assertThrows<IllegalStateException> {
                    deliveryService.unassignRobot(delivery.getDeliveryId())
                }

            assertTrue(exception.message!!.contains("배달 출발 전 상태에서만 배차 취소가 가능합니다"))
        }
    }

    // Helper functions
    private fun createDelivery(id: Long): Delivery =
        Delivery(
            id = id,
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

    private fun createRobot(
        id: Long = 1L,
        name: String = "로봇-1",
    ): Robot =
        Robot(
            id = id,
            name = name,
            status = RobotStatus.OFF_DUTY,
            battery = 100,
            location = Location(latitude = 37.5665, longitude = 126.9780),
        )

    private fun createDeliveryInAssignedState(): Delivery {
        val delivery = createDelivery(1L)
        delivery.assignRobot(RobotId(1L))
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createDeliveryInPickupArrivedState(): Delivery {
        val delivery = createDeliveryInAssignedState()
        delivery.arrived()
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createDeliveryInDeliveryArrivedState(): Delivery {
        val delivery = createDeliveryInPickupArrivedState()
        delivery.openDoor()
        delivery.startDelivery()
        delivery.arrived()
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createDeliveryInPickingUpState(): Delivery {
        val delivery = createDeliveryInPickupArrivedState()
        delivery.openDoor()
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createDeliveryInDroppingOffState(): Delivery {
        val delivery = createDeliveryInDeliveryArrivedState()
        delivery.openDoor()
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createDeliveryInReturningOffState(): Delivery {
        val delivery = createDeliveryInDroppingOffState()
        delivery.cancel()
        delivery.arrived()
        delivery.openDoor()
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createDeliveryInDeliveringState(): Delivery {
        val delivery = createDeliveryInPickingUpState()
        delivery.startDelivery()
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createDeliveryInCompletedState(): Delivery {
        val delivery = createDeliveryInDroppingOffState()
        delivery.complete()
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createDeliveryInCanceledState(): Delivery {
        val delivery = createDelivery(1L)
        delivery.cancel()
        delivery.pullDomainEvents()
        return delivery
    }

    private fun createRobotWithDelivery(delivery: Delivery): Robot {
        val robot = createRobot()
        robot.startDuty()
        robot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)
        robot.pullDomainEvents()
        return robot
    }
}
