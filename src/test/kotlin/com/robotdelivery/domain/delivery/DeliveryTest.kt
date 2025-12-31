package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.delivery.event.DeliveryApproachingEvent
import com.robotdelivery.domain.delivery.event.DeliveryCanceledEvent
import com.robotdelivery.domain.delivery.event.DeliveryCompletedEvent
import com.robotdelivery.domain.delivery.event.DeliveryCreatedEvent
import com.robotdelivery.domain.delivery.event.DeliveryReturnCompletedEvent
import com.robotdelivery.domain.delivery.event.DeliveryReturnStartedEvent
import com.robotdelivery.domain.delivery.event.DeliveryRobotAssignedEvent
import com.robotdelivery.domain.delivery.event.DeliveryRobotReassignedEvent
import com.robotdelivery.domain.delivery.event.DeliveryRobotUnassignedEvent
import com.robotdelivery.domain.delivery.event.DeliveryStartedEvent
import com.robotdelivery.domain.delivery.vo.DeliveryStatus
import com.robotdelivery.domain.delivery.vo.Destination
import com.robotdelivery.domain.delivery.vo.DestinationType
import com.robotdelivery.domain.robot.vo.RouteResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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

    private val defaultRouteResult = RouteResult.of(toPickupSeconds = 300, toDeliverySeconds = 600)

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

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(delivery.assignedRobotId).isNull()
        }

        @Test
        @DisplayName("새 배달이 persist되면 DeliveryCreatedEvent가 발생한다")
        fun `새 배달이 persist되면 DeliveryCreatedEvent가 발생한다`() {
            val delivery =
                Delivery(
                    id = 1L,
                    pickupDestination = pickupDestination,
                    deliveryDestination = deliveryDestination,
                    phoneNumber = "010-1234-5678",
                ).apply { isNew = true }

            delivery.onPostPersist()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(DeliveryCreatedEvent::class.java)
        }

        @Test
        @DisplayName("onPostPersist는 한 번만 이벤트를 등록한다")
        fun `onPostPersist는 한 번만 이벤트를 등록한다`() {
            val delivery =
                Delivery(
                    id = 1L,
                    pickupDestination = pickupDestination,
                    deliveryDestination = deliveryDestination,
                    phoneNumber = "010-1234-5678",
                ).apply { isNew = true }

            delivery.onPostPersist()
            delivery.onPostPersist()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
        }

        @Test
        @DisplayName("기존 배달 로드 시 이벤트가 발생하지 않는다")
        fun `기존 배달 로드 시 이벤트가 발생하지 않는다`() {
            val delivery = createDelivery(id = 1L)

            val events = delivery.pullDomainEvents()

            assertThat(events).isEmpty()
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

            delivery.assignRobot(robotId, defaultRouteResult)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.ASSIGNED)
            assertThat(delivery.assignedRobotId).isEqualTo(robotId)
        }

        @Test
        @DisplayName("로봇 할당 시 DeliveryRobotAssignedEvent가 발생한다")
        fun `로봇 할당 시 DeliveryRobotAssignedEvent가 발생한다`() {
            val delivery = createDelivery()
            val robotId = RobotId(1L)

            delivery.assignRobot(robotId, defaultRouteResult)
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            val event = events[0] as DeliveryRobotAssignedEvent
            assertThat(event.robotId).isEqualTo(robotId)
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 로봇을 할당하면 예외가 발생한다")
        fun `PENDING이 아닌 상태에서 로봇을 할당하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)

            assertThatThrownBy { delivery.assignRobot(RobotId(2L), defaultRouteResult) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("대기 상태의 배달만 로봇 배차가 가능합니다")
        }
    }

    @Nested
    @DisplayName("도착 처리 테스트")
    inner class ArrivedTest {
        @Test
        @DisplayName("ASSIGNED 상태에서 도착하면 PICKUP_ARRIVED가 된다")
        fun `ASSIGNED 상태에서 도착하면 PICKUP_ARRIVED가 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)

            delivery.arrived()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PICKUP_ARRIVED)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 도착하면 DELIVERY_ARRIVED가 된다")
        fun `DELIVERING 상태에서 도착하면 DELIVERY_ARRIVED가 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            delivery.arrived()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.DELIVERY_ARRIVED)
        }

        @Test
        @DisplayName("PENDING 상태에서 도착 처리하면 예외가 발생한다")
        fun `PENDING 상태에서 도착 처리하면 예외가 발생한다`() {
            val delivery = createDelivery()

            assertThatThrownBy { delivery.arrived() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("도착 처리할 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("문 열기 테스트")
    inner class OpenDoorTest {
        @Test
        @DisplayName("PICKUP_ARRIVED 상태에서 문을 열면 PICKING_UP이 된다")
        fun `PICKUP_ARRIVED 상태에서 문을 열면 PICKING_UP이 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()

            delivery.openDoor()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PICKING_UP)
        }

        @Test
        @DisplayName("DELIVERY_ARRIVED 상태에서 문을 열면 DROPPING_OFF가 된다")
        fun `DELIVERY_ARRIVED 상태에서 문을 열면 DROPPING_OFF가 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()

            delivery.openDoor()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.DROPPING_OFF)
        }

        @Test
        @DisplayName("PENDING 상태에서 문을 열면 예외가 발생한다")
        fun `PENDING 상태에서 문을 열면 예외가 발생한다`() {
            val delivery = createDelivery()

            assertThatThrownBy { delivery.openDoor() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("문을 열 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("배송 시작 테스트")
    inner class StartDeliveryTest {
        @Test
        @DisplayName("PICKING_UP 상태에서 배송을 시작할 수 있다")
        fun `PICKING_UP 상태에서 배송을 시작할 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()

            delivery.startDelivery()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.DELIVERING)
        }

        @Test
        @DisplayName("배송 시작 시 DeliveryStartedEvent가 발생한다")
        fun `배송 시작 시 DeliveryStartedEvent가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.pullDomainEvents()

            delivery.startDelivery()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(DeliveryStartedEvent::class.java)
        }

        @Test
        @DisplayName("ASSIGNED 상태에서 배송을 시작하면 예외가 발생한다")
        fun `ASSIGNED 상태에서 배송을 시작하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)

            assertThatThrownBy { delivery.startDelivery() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("픽업 중 상태에서만 배송을 시작할 수 있습니다")
        }
    }

    @Nested
    @DisplayName("배달 완료 테스트")
    inner class CompleteTest {
        private fun createDeliveryInDroppingOffState(): Delivery {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
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

            assertThat(delivery.status).isEqualTo(DeliveryStatus.COMPLETED)
        }

        @Test
        @DisplayName("배달 완료 시 DeliveryCompletedEvent가 발생한다")
        fun `배달 완료 시 DeliveryCompletedEvent가 발생한다`() {
            val delivery = createDeliveryInDroppingOffState()
            delivery.pullDomainEvents()

            delivery.complete()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(DeliveryCompletedEvent::class.java)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 완료하면 예외가 발생한다")
        fun `DELIVERING 상태에서 완료하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            assertThatThrownBy { delivery.complete() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 완료 처리할 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("배차 취소 테스트")
    inner class UnassignRobotTest {
        @Test
        @DisplayName("ASSIGNED 상태에서 배차를 취소할 수 있다")
        fun `ASSIGNED 상태에서 배차를 취소할 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)

            delivery.unassignRobot()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(delivery.assignedRobotId).isNull()
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태에서 배차를 취소할 수 있다")
        fun `PICKUP_ARRIVED 상태에서 배차를 취소할 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()

            delivery.unassignRobot()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(delivery.assignedRobotId).isNull()
        }

        @Test
        @DisplayName("PICKING_UP 상태에서 배차를 취소할 수 있다")
        fun `PICKING_UP 상태에서 배차를 취소할 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()

            delivery.unassignRobot()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(delivery.assignedRobotId).isNull()
        }

        @Test
        @DisplayName("배차 취소 시 DeliveryRobotUnassignedEvent가 발생한다")
        fun `배차 취소 시 DeliveryRobotUnassignedEvent가 발생한다`() {
            val delivery = createDelivery()
            val robotId = RobotId(1L)
            delivery.assignRobot(robotId, defaultRouteResult)
            delivery.pullDomainEvents()

            delivery.unassignRobot()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            val event = events[0] as DeliveryRobotUnassignedEvent
            assertThat(event.robotId).isEqualTo(robotId)
            assertThat(event.deliveryId).isEqualTo(delivery.getDeliveryId())
        }

        @Test
        @DisplayName("PENDING 상태에서 배차 취소하면 예외가 발생한다")
        fun `PENDING 상태에서 배차 취소하면 예외가 발생한다`() {
            val delivery = createDelivery()

            assertThatThrownBy { delivery.unassignRobot() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 출발 전 상태에서만 배차 취소가 가능합니다.")
        }

        @Test
        @DisplayName("DELIVERING 상태에서 배차 취소하면 예외가 발생한다")
        fun `DELIVERING 상태에서 배차 취소하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            assertThatThrownBy { delivery.unassignRobot() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 출발 전 상태에서만 배차 취소가 가능합니다")
        }

        @Test
        @DisplayName("COMPLETED 상태에서 배차 취소하면 예외가 발생한다")
        fun `COMPLETED 상태에서 배차 취소하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()
            delivery.complete()

            assertThatThrownBy { delivery.unassignRobot() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 출발 전 상태에서만 배차 취소가 가능합니다")
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

            assertThat(delivery.status).isEqualTo(DeliveryStatus.CANCELED)
        }

        @Test
        @DisplayName("ASSIGNED 상태에서 취소할 수 있다")
        fun `ASSIGNED 상태에서 취소할 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)

            delivery.cancel()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.CANCELED)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 취소하면 RETURNING 상태가 된다")
        fun `DELIVERING 상태에서 취소하면 RETURNING 상태가 된다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            delivery.cancel()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.RETURNING)
        }

        @Test
        @DisplayName("COMPLETED 상태에서 취소하면 예외가 발생한다")
        fun `COMPLETED 상태에서 취소하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()
            delivery.complete()

            assertThatThrownBy { delivery.cancel() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("취소할 수 없는 상태입니다")
        }

        @Test
        @DisplayName("취소 시 DeliveryCanceledEvent가 발생한다")
        fun `취소 시 DeliveryCanceledEvent가 발생한다`() {
            val delivery = createDelivery()
            delivery.pullDomainEvents()

            delivery.cancel()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(DeliveryCanceledEvent::class.java)
            val event = events[0] as DeliveryCanceledEvent
            assertThat(event.requiresReturn).isFalse()
        }

        @Test
        @DisplayName("DELIVERING 상태에서 취소 시 DeliveryReturnStartedEvent와 DeliveryCanceledEvent가 발생한다")
        fun `DELIVERING 상태에서 취소 시 DeliveryReturnStartedEvent와 DeliveryCanceledEvent가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.pullDomainEvents()

            delivery.cancel()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(2)
            assertThat(events[0]).isInstanceOf(DeliveryReturnStartedEvent::class.java)
            assertThat(events[1]).isInstanceOf(DeliveryCanceledEvent::class.java)
            val cancelEvent = events[1] as DeliveryCanceledEvent
            assertThat(cancelEvent.requiresReturn).isTrue()
        }
    }

    @Nested
    @DisplayName("접근 알림 테스트")
    inner class ApproachingTest {
        @Test
        @DisplayName("ASSIGNED 상태에서 접근 알림을 보낼 수 있다")
        fun `ASSIGNED 상태에서 접근 알림을 보낼 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.pullDomainEvents()

            delivery.approaching()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(DeliveryApproachingEvent::class.java)
            val event = events[0] as DeliveryApproachingEvent
            assertThat(event.destinationType).isEqualTo(DestinationType.PICKUP)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 접근 알림을 보낼 수 있다")
        fun `DELIVERING 상태에서 접근 알림을 보낼 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.pullDomainEvents()

            delivery.approaching()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(DeliveryApproachingEvent::class.java)
            val event = events[0] as DeliveryApproachingEvent
            assertThat(event.destinationType).isEqualTo(DestinationType.DELIVERY)
        }

        @Test
        @DisplayName("RETURNING 상태에서 접근 알림을 보낼 수 있다")
        fun `RETURNING 상태에서 접근 알림을 보낼 수 있다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.cancel()
            delivery.pullDomainEvents()

            delivery.approaching()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(DeliveryApproachingEvent::class.java)
            val event = events[0] as DeliveryApproachingEvent
            assertThat(event.destinationType).isEqualTo(DestinationType.RETURN)
        }

        @Test
        @DisplayName("PENDING 상태에서 접근 알림을 보내면 예외가 발생한다")
        fun `PENDING 상태에서 접근 알림을 보내면 예외가 발생한다`() {
            val delivery = createDelivery()

            assertThatThrownBy { delivery.approaching() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("접근 알림을 보낼 수 없는 상태입니다")
        }

        @Test
        @DisplayName("COMPLETED 상태에서 접근 알림을 보내면 예외가 발생한다")
        fun `COMPLETED 상태에서 접근 알림을 보내면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()
            delivery.complete()

            assertThatThrownBy { delivery.approaching() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("접근 알림을 보낼 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("회수 완료 테스트")
    inner class CompleteReturnTest {
        private fun createDeliveryInReturningOffState(): Delivery {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.cancel()
            delivery.arrived()
            delivery.openDoor()
            return delivery
        }

        @Test
        @DisplayName("RETURNING_OFF 상태에서 회수를 완료할 수 있다")
        fun `RETURNING_OFF 상태에서 회수를 완료할 수 있다`() {
            val delivery = createDeliveryInReturningOffState()

            delivery.completeReturn()

            assertThat(delivery.status).isEqualTo(DeliveryStatus.RETURN_COMPLETED)
        }

        @Test
        @DisplayName("회수 완료 시 DeliveryReturnCompletedEvent가 발생한다")
        fun `회수 완료 시 DeliveryReturnCompletedEvent가 발생한다`() {
            val delivery = createDeliveryInReturningOffState()
            delivery.pullDomainEvents()

            delivery.completeReturn()
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(DeliveryReturnCompletedEvent::class.java)
        }

        @Test
        @DisplayName("RETURNING 상태에서 회수 완료하면 예외가 발생한다")
        fun `RETURNING 상태에서 회수 완료하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.cancel()

            assertThatThrownBy { delivery.completeReturn() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("회수 완료 처리할 수 없는 상태입니다")
        }

        @Test
        @DisplayName("DROPPING_OFF 상태에서 회수 완료하면 예외가 발생한다")
        fun `DROPPING_OFF 상태에서 회수 완료하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()

            assertThatThrownBy { delivery.completeReturn() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("회수 완료 처리할 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("현재 목적지 테스트")
    inner class GetCurrentDestinationTest {
        @Test
        @DisplayName("PENDING 상태에서는 픽업 목적지가 현재 목적지다")
        fun `PENDING 상태에서는 픽업 목적지가 현재 목적지다`() {
            val delivery = createDelivery()

            assertThat(delivery.getCurrentDestination()).isEqualTo(pickupDestination)
        }

        @Test
        @DisplayName("ASSIGNED 상태에서는 픽업 목적지가 현재 목적지다")
        fun `ASSIGNED 상태에서는 픽업 목적지가 현재 목적지다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)

            assertThat(delivery.getCurrentDestination()).isEqualTo(pickupDestination)
        }

        @Test
        @DisplayName("DELIVERING 상태에서는 배달 목적지가 현재 목적지다")
        fun `DELIVERING 상태에서는 배달 목적지가 현재 목적지다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            assertThat(delivery.getCurrentDestination()).isEqualTo(deliveryDestination)
        }

        @Test
        @DisplayName("RETURNING 상태에서는 픽업 목적지가 현재 목적지다")
        fun `RETURNING 상태에서는 픽업 목적지가 현재 목적지다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.cancel()

            assertThat(delivery.getCurrentDestination()).isEqualTo(pickupDestination)
        }
    }

    @Nested
    @DisplayName("배차 변경 테스트")
    inner class ReassignRobotTest {
        @Test
        @DisplayName("ASSIGNED 상태에서 배차 변경할 수 있다")
        fun `ASSIGNED 상태에서 배차 변경할 수 있다`() {
            val delivery = createDelivery()
            val oldRobotId = RobotId(1L)
            val newRobotId = RobotId(2L)
            delivery.assignRobot(oldRobotId, defaultRouteResult)

            delivery.reassignRobot(newRobotId)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.ASSIGNED)
            assertThat(delivery.assignedRobotId).isEqualTo(newRobotId)
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태에서 배차 변경할 수 있다")
        fun `PICKUP_ARRIVED 상태에서 배차 변경할 수 있다`() {
            val delivery = createDelivery()
            val oldRobotId = RobotId(1L)
            val newRobotId = RobotId(2L)
            delivery.assignRobot(oldRobotId, defaultRouteResult)
            delivery.arrived()

            delivery.reassignRobot(newRobotId)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PICKUP_ARRIVED)
            assertThat(delivery.assignedRobotId).isEqualTo(newRobotId)
        }

        @Test
        @DisplayName("PICKING_UP 상태에서 배차 변경할 수 있다")
        fun `PICKING_UP 상태에서 배차 변경할 수 있다`() {
            val delivery = createDelivery()
            val oldRobotId = RobotId(1L)
            val newRobotId = RobotId(2L)
            delivery.assignRobot(oldRobotId, defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()

            delivery.reassignRobot(newRobotId)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.PICKING_UP)
            assertThat(delivery.assignedRobotId).isEqualTo(newRobotId)
        }

        @Test
        @DisplayName("DELIVERING 상태에서 배차 변경할 수 있다")
        fun `DELIVERING 상태에서 배차 변경할 수 있다`() {
            val delivery = createDelivery()
            val oldRobotId = RobotId(1L)
            val newRobotId = RobotId(2L)
            delivery.assignRobot(oldRobotId, defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            delivery.reassignRobot(newRobotId)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.DELIVERING)
            assertThat(delivery.assignedRobotId).isEqualTo(newRobotId)
        }

        @Test
        @DisplayName("DELIVERY_ARRIVED 상태에서 배차 변경할 수 있다")
        fun `DELIVERY_ARRIVED 상태에서 배차 변경할 수 있다`() {
            val delivery = createDelivery()
            val oldRobotId = RobotId(1L)
            val newRobotId = RobotId(2L)
            delivery.assignRobot(oldRobotId, defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()

            delivery.reassignRobot(newRobotId)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.DELIVERY_ARRIVED)
            assertThat(delivery.assignedRobotId).isEqualTo(newRobotId)
        }

        @Test
        @DisplayName("DROPPING_OFF 상태에서 배차 변경할 수 있다")
        fun `DROPPING_OFF 상태에서 배차 변경할 수 있다`() {
            val delivery = createDelivery()
            val oldRobotId = RobotId(1L)
            val newRobotId = RobotId(2L)
            delivery.assignRobot(oldRobotId, defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()

            delivery.reassignRobot(newRobotId)

            assertThat(delivery.status).isEqualTo(DeliveryStatus.DROPPING_OFF)
            assertThat(delivery.assignedRobotId).isEqualTo(newRobotId)
        }

        @Test
        @DisplayName("배차 변경 시 DeliveryRobotReassignedEvent가 발생한다")
        fun `배차 변경 시 DeliveryRobotReassignedEvent가 발생한다`() {
            val delivery = createDelivery()
            val oldRobotId = RobotId(1L)
            val newRobotId = RobotId(2L)
            delivery.assignRobot(oldRobotId, defaultRouteResult)
            delivery.pullDomainEvents()

            delivery.reassignRobot(newRobotId)
            val events = delivery.pullDomainEvents()

            assertThat(events).hasSize(1)
            val event = events[0] as DeliveryRobotReassignedEvent
            assertThat(event.previousRobotId).isEqualTo(oldRobotId)
            assertThat(event.newRobotId).isEqualTo(newRobotId)
        }

        @Test
        @DisplayName("PENDING 상태에서 배차 변경하면 예외가 발생한다")
        fun `PENDING 상태에서 배차 변경하면 예외가 발생한다`() {
            val delivery = createDelivery()
            val newRobotId = RobotId(2L)

            assertThatThrownBy { delivery.reassignRobot(newRobotId) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배차 변경이 불가능한 상태입니다")
        }

        @Test
        @DisplayName("COMPLETED 상태에서 배차 변경하면 예외가 발생한다")
        fun `COMPLETED 상태에서 배차 변경하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()
            delivery.complete()

            assertThatThrownBy { delivery.reassignRobot(RobotId(2L)) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배차 변경이 불가능한 상태입니다")
        }

        @Test
        @DisplayName("CANCELED 상태에서 배차 변경하면 예외가 발생한다")
        fun `CANCELED 상태에서 배차 변경하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.cancel()

            assertThatThrownBy { delivery.reassignRobot(RobotId(2L)) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배차 변경이 불가능한 상태입니다")
        }

        @Test
        @DisplayName("RETURNING 상태에서 배차 변경하면 예외가 발생한다")
        fun `RETURNING 상태에서 배차 변경하면 예외가 발생한다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.cancel()

            assertThatThrownBy { delivery.reassignRobot(RobotId(2L)) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배차 변경이 불가능한 상태입니다")
        }

        @Test
        @DisplayName("동일한 로봇으로 배차 변경하면 예외가 발생한다")
        fun `동일한 로봇으로 배차 변경하면 예외가 발생한다`() {
            val delivery = createDelivery()
            val robotId = RobotId(1L)
            delivery.assignRobot(robotId, defaultRouteResult)

            assertThatThrownBy { delivery.reassignRobot(robotId) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("동일한 로봇으로는 배차 변경할 수 없습니다")
        }
    }

    @Nested
    @DisplayName("활성 상태 테스트")
    inner class IsActiveTest {
        @Test
        @DisplayName("PENDING 상태는 활성 상태다")
        fun `PENDING 상태는 활성 상태다`() {
            val delivery = createDelivery()

            assertThat(delivery.isActive()).isTrue()
        }

        @Test
        @DisplayName("DELIVERING 상태는 활성 상태다")
        fun `DELIVERING 상태는 활성 상태다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()

            assertThat(delivery.isActive()).isTrue()
        }

        @Test
        @DisplayName("COMPLETED 상태는 활성 상태가 아니다")
        fun `COMPLETED 상태는 활성 상태가 아니다`() {
            val delivery = createDelivery()
            delivery.assignRobot(RobotId(1L), defaultRouteResult)
            delivery.arrived()
            delivery.openDoor()
            delivery.startDelivery()
            delivery.arrived()
            delivery.openDoor()
            delivery.complete()

            assertThat(delivery.isActive()).isFalse()
        }

        @Test
        @DisplayName("CANCELED 상태는 활성 상태가 아니다")
        fun `CANCELED 상태는 활성 상태가 아니다`() {
            val delivery = createDelivery()
            delivery.cancel()

            assertThat(delivery.isActive()).isFalse()
        }
    }
}
