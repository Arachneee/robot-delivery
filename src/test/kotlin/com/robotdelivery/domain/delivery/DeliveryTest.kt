package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.delivery.event.DeliveryCompletedEvent
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import com.robotdelivery.domain.delivery.event.DeliveryRobotAssignedEvent
import com.robotdelivery.domain.delivery.event.DeliveryStartedEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Delivery 테스트")
class DeliveryTest {
    private lateinit var pickupDestination: Destination
    private lateinit var deliveryDestination: Destination

    @BeforeEach
    fun setUp() {
        pickupDestination =
            Destination(
                address = "서울시 중구 세종대로 110",
                addressDetail = "시청역 1번 출구",
                location = Location(latitude = 37.5665, longitude = 126.9780),
            )
        deliveryDestination =
            Destination(
                address = "서울시 강남구 테헤란로 1",
                addressDetail = "강남역 2번 출구",
                location = Location(latitude = 37.4979, longitude = 127.0276),
            )
    }

    private fun createDelivery(id: Long = 1L): Delivery =
        Delivery(
            id = id,
            pickupDestination = pickupDestination,
            deliveryDestination = deliveryDestination,
            phoneNumber = "010-1234-5678",
        )

    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("배달을 생성하면 PENDING 상태이다")
        fun `배달을 생성하면 PENDING 상태이다`() {
            val delivery = createDelivery()

            assertEquals(DeliveryStatus.PENDING, delivery.status)
            assertNull(delivery.assignedRobotId)
        }

        @Test
        @DisplayName("새 배달이 persist되면 DeliveryCreatedEvent가 발생한다")
        fun `새 배달이 persist되면 DeliveryCreatedEvent가 발생한다`() {
            val delivery =
                Delivery(
                    id = 0L, // 새로운 엔티티
                    pickupDestination = pickupDestination,
                    deliveryDestination = deliveryDestination,
                    phoneNumber = "010-1234-5678",
                )

            // JPA @PostPersist 콜백 시뮬레이션
            delivery.onPostPersist()
            val events = delivery.pullDomainEvents()

            assertEquals(1, events.size)
            assertTrue(events[0] is DeliveryCreatedEvent)
        }

        @Test
        @DisplayName("onPostPersist는 한 번만 이벤트를 등록한다")
        fun `onPostPersist는 한 번만 이벤트를 등록한다`() {
            val delivery =
                Delivery(
                    id = 0L,
                    pickupDestination = pickupDestination,
                    deliveryDestination = deliveryDestination,
                    phoneNumber = "010-1234-5678",
                )

            delivery.onPostPersist()
            delivery.onPostPersist() // 두 번째 호출
            val events = delivery.pullDomainEvents()

            assertEquals(1, events.size) // 여전히 1개만
        }

        @Test
        @DisplayName("기존 배달 로드 시 이벤트가 발생하지 않는다")
        fun `기존 배달 로드 시 이벤트가 발생하지 않는다`() {
            val delivery = createDelivery(id = 1L)

            val events = delivery.pullDomainEvents()

            assertTrue(events.isEmpty())
        }
    }

    @Nested
    @DisplayName("로봇 할당 테스트")
    inner class AssignRobotTest {
        @Test
        @DisplayName("PENDING 상태에서 로봇을 할당할 수 있다")
        fun `PENDING 상태에서 로봇을 할당할 수 있다`() {
            val delivery = createDelivery()
            val robotId = RobotId(1L)

            delivery.assignRobot(robotId)

            assertEquals(DeliveryStatus.ASSIGNED, delivery.status)
            assertEquals(robotId, delivery.assignedRobotId)
        }

        @Test
        @DisplayName("로봇 할당 시 DeliveryRobotAssignedEvent가 발생한다")
        fun `로봇 할당 시 DeliveryRobotAssignedEvent가 발생한다`() {
            val delivery = createDelivery()
            val robotId = RobotId(1L)

            delivery.assignRobot(robotId)
            val events = delivery.pullDomainEvents()

            assertEquals(1, events.size)
            val event = events[0] as DeliveryRobotAssignedEvent
            assertEquals(robotId, event.robotId)
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 로봇을 할당하면 예외가 발생한다")
        fun `PENDING이 아닌 상태에서 로봇을 할당하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))

            val exception =
                assertThrows<IllegalArgumentException> {
                    delivery.assignRobot(RobotId(2L))
                }
            assertTrue(exception.message!!.contains("대기 상태의 배달만 로봇 배차가 가능합니다"))
        }
    }

    @Nested
    @DisplayName("도착 처리 테스트")
    inner class ArrivedTest {
        @Test
        @DisplayName("ASSIGNED 상태에서 도착하면 PICKUP_ARRIVED가 된다")
        fun `ASSIGNED 상태에서 도착하면 PICKUP_ARRIVED가 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))

            delivery.arrived()

            assertEquals(DeliveryStatus.PICKUP_ARRIVED, delivery.status)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 도착하면 DELIVERY_ARRIVED가 된다")
        fun `DELIVERING 상태에서 도착하면 DELIVERY_ARRIVED가 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived() // PICKUP_ARRIVED
            delivery.openDoor() // PICKING_UP
            delivery.startDelivery() // DELIVERING

            delivery.arrived()

            assertEquals(DeliveryStatus.DELIVERY_ARRIVED, delivery.status)
        }

        @Test
        @DisplayName("PENDING 상태에서 도착 처리하면 예외가 발생한다")
        fun `PENDING 상태에서 도착 처리하면 예외가 발생한다`() {
            val delivery = createDelivery()

            val exception =
                assertThrows<IllegalStateException> {
                    delivery.arrived()
                }
            assertTrue(exception.message!!.contains("도착 처리할 수 없는 상태입니다"))
        }
    }

    @Nested
    @DisplayName("문 열기 테스트")
    inner class OpenDoorTest {
        @Test
        @DisplayName("PICKUP_ARRIVED 상태에서 문을 열면 PICKING_UP이 된다")
        fun `PICKUP_ARRIVED 상태에서 문을 열면 PICKING_UP이 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()

            delivery.openDoor()

            assertEquals(DeliveryStatus.PICKING_UP, delivery.status)
        }

        @Test
        @DisplayName("DELIVERY_ARRIVED 상태에서 문을 열면 DROPPING_OFF가 된다")
        fun `DELIVERY_ARRIVED 상태에서 문을 열면 DROPPING_OFF가 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()

            delivery.openDoor()

            assertEquals(DeliveryStatus.DROPPING_OFF, delivery.status)
        }

        @Test
        @DisplayName("PENDING 상태에서 문을 열면 예외가 발생한다")
        fun `PENDING 상태에서 문을 열면 예외가 발생한다`() {
            val delivery = createDelivery()

            val exception =
                assertThrows<IllegalStateException> {
                    delivery.openDoor()
                }
            assertTrue(exception.message!!.contains("문을 열 수 없는 상태입니다"))
        }
    }

    @Nested
    @DisplayName("배송 시작 테스트")
    inner class StartDeliveryTest {
        @Test
        @DisplayName("PICKING_UP 상태에서 배송을 시작할 수 있다")
        fun `PICKING_UP 상태에서 배송을 시작할 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()

            delivery.startDelivery()

            assertEquals(DeliveryStatus.DELIVERING, delivery.status)
        }

        @Test
        @DisplayName("배송 시작 시 DeliveryStartedEvent가 발생한다")
        fun `배송 시작 시 DeliveryStartedEvent가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.pullDomainEvents() // 이전 이벤트 클리어

            delivery.startDelivery()
            val events = delivery.pullDomainEvents()

            assertEquals(1, events.size)
            assertTrue(events[0] is DeliveryStartedEvent)
        }

        @Test
        @DisplayName("ASSIGNED 상태에서 배송을 시작하면 예외가 발생한다")
        fun `ASSIGNED 상태에서 배송을 시작하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))

            val exception =
                assertThrows<IllegalArgumentException> {
                    delivery.startDelivery()
                }
            assertTrue(exception.message!!.contains("픽업 중 상태에서만 배송을 시작할 수 있습니다"))
        }
    }

    @Nested
    @DisplayName("배달 완료 테스트")
    inner class CompleteTest {
        private fun createDeliveryInDroppingOffState(): Delivery {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()
            return delivery
        }

        @Test
        @DisplayName("DROPPING_OFF 상태에서 배달을 완료할 수 있다")
        fun `DROPPING_OFF 상태에서 배달을 완료할 수 있다`() {
            val delivery = createDeliveryInDroppingOffState()

            delivery.complete()

            assertEquals(DeliveryStatus.COMPLETED, delivery.status)
        }

        @Test
        @DisplayName("배달 완료 시 DeliveryCompletedEvent가 발생한다")
        fun `배달 완료 시 DeliveryCompletedEvent가 발생한다`() {
            val delivery = createDeliveryInDroppingOffState()
            delivery.pullDomainEvents()

            delivery.complete()
            val events = delivery.pullDomainEvents()

            assertEquals(1, events.size)
            assertTrue(events[0] is DeliveryCompletedEvent)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 완료하면 예외가 발생한다")
        fun `DELIVERING 상태에서 완료하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            val exception =
                assertThrows<IllegalArgumentException> {
                    delivery.complete()
                }
            assertTrue(exception.message!!.contains("배달 완료 처리할 수 없는 상태입니다"))
        }
    }

    @Nested
    @DisplayName("취소 테스트")
    inner class CancelTest {
        @Test
        @DisplayName("PENDING 상태에서 취소할 수 있다")
        fun `PENDING 상태에서 취소할 수 있다`() {
            val delivery = createDelivery()

            delivery.cancel()

            assertEquals(DeliveryStatus.CANCELED, delivery.status)
        }

        @Test
        @DisplayName("ASSIGNED 상태에서 취소할 수 있다")
        fun `ASSIGNED 상태에서 취소할 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))

            delivery.cancel()

            assertEquals(DeliveryStatus.CANCELED, delivery.status)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 취소하면 RETURNING 상태가 된다")
        fun `DELIVERING 상태에서 취소하면 RETURNING 상태가 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            delivery.cancel()

            assertEquals(DeliveryStatus.RETURNING, delivery.status)
        }

        @Test
        @DisplayName("COMPLETED 상태에서 취소하면 예외가 발생한다")
        fun `COMPLETED 상태에서 취소하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()
            delivery.complete()

            val exception =
                assertThrows<IllegalStateException> {
                    delivery.cancel()
                }
            assertTrue(exception.message!!.contains("취소할 수 없는 상태입니다"))
        }
    }

    @Nested
    @DisplayName("현재 목적지 테스트")
    inner class GetCurrentDestinationTest {
        @Test
        @DisplayName("PENDING 상태에서는 목적지가 없다")
        fun `PENDING 상태에서는 목적지가 없다`() {
            val delivery = createDelivery()

            assertNull(delivery.getCurrentDestination())
        }

        @Test
        @DisplayName("ASSIGNED 상태에서는 픽업 목적지가 현재 목적지다")
        fun `ASSIGNED 상태에서는 픽업 목적지가 현재 목적지다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))

            assertEquals(pickupDestination, delivery.getCurrentDestination())
        }

        @Test
        @DisplayName("DELIVERING 상태에서는 배달 목적지가 현재 목적지다")
        fun `DELIVERING 상태에서는 배달 목적지가 현재 목적지다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            assertEquals(deliveryDestination, delivery.getCurrentDestination())
        }

        @Test
        @DisplayName("RETURNING 상태에서는 픽업 목적지가 현재 목적지다")
        fun `RETURNING 상태에서는 픽업 목적지가 현재 목적지다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.cancel()

            assertEquals(pickupDestination, delivery.getCurrentDestination())
        }
    }

    @Nested
    @DisplayName("활성 상태 테스트")
    inner class IsActiveTest {
        @Test
        @DisplayName("PENDING 상태는 활성 상태다")
        fun `PENDING 상태는 활성 상태다`() {
            val delivery = createDelivery()

            assertTrue(delivery.isActive())
        }

        @Test
        @DisplayName("DELIVERING 상태는 활성 상태다")
        fun `DELIVERING 상태는 활성 상태다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            assertTrue(delivery.isActive())
        }

        @Test
        @DisplayName("COMPLETED 상태는 활성 상태가 아니다")
        fun `COMPLETED 상태는 활성 상태가 아니다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L))
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()
            delivery.complete()

            assertFalse(delivery.isActive())
        }

        @Test
        @DisplayName("CANCELED 상태는 활성 상태가 아니다")
        fun `CANCELED 상태는 활성 상태가 아니다`() {
            val delivery = createDelivery()
            delivery.cancel()

            assertFalse(delivery.isActive())
        }
    }
}
