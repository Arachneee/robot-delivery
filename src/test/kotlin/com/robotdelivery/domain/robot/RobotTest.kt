package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.robot.event.RobotApproachingEvent
import com.robotdelivery.domain.robot.event.RobotArrivedEvent
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent
import com.robotdelivery.domain.robot.event.RobotDestinationChangedEvent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Robot 테스트")
class RobotTest {
    private fun createRobot(
        id: Long = 1L,
        name: String = "로봇-001",
        status: RobotStatus = RobotStatus.OFF_DUTY,
        drivingStatus: RobotDrivingStatus = RobotDrivingStatus.ARRIVED,
        currentDeliveryId: DeliveryId? = null,
        destination: Location? = null,
    ): Robot =
        Robot(
            id = id,
            name = name,
            status = status,
            drivingStatus = drivingStatus,
            currentDeliveryId = currentDeliveryId,
            destination = destination,
        )

    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("로봇을 생성하면 OFF_DUTY 상태이다")
        fun `로봇을 생성하면 OFF_DUTY 상태이다`() {
            val robot = createRobot()

            assertThat(robot.status).isEqualTo(RobotStatus.OFF_DUTY)
            assertThat(robot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("로봇 ID를 조회할 수 있다")
        fun `로봇 ID를 조회할 수 있다`() {
            val robot = createRobot(id = 5L)

            assertThat(robot.getRobotId().value).isEqualTo(5L)
        }
    }

    @Nested
    @DisplayName("출근 테스트")
    inner class StartDutyTest {
        @Test
        @DisplayName("OFF_DUTY 상태에서 출근할 수 있다")
        fun `OFF_DUTY 상태에서 출근할 수 있다`() {
            val robot = createRobot()

            robot.startDuty()

            assertThat(robot.status).isEqualTo(RobotStatus.READY)
        }

        @Test
        @DisplayName("출근 시 RobotBecameAvailableEvent가 발생한다")
        fun `출근 시 RobotBecameAvailableEvent가 발생한다`() {
            val robot = createRobot()

            robot.startDuty()
            val events = robot.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(RobotBecameAvailableEvent::class.java)
        }
    }

    @Nested
    @DisplayName("퇴근 테스트")
    inner class EndDutyTest {
        @Test
        @DisplayName("READY 상태에서 퇴근할 수 있다")
        fun `READY 상태에서 퇴근할 수 있다`() {
            val robot = createRobot(status = RobotStatus.READY)

            robot.endDuty()

            assertThat(robot.status).isEqualTo(RobotStatus.OFF_DUTY)
        }

        @Test
        @DisplayName("퇴근 시 이벤트가 발생하지 않는다")
        fun `퇴근 시 이벤트가 발생하지 않는다`() {
            val robot = createRobot(status = RobotStatus.READY)

            robot.endDuty()
            val events = robot.pullDomainEvents()

            assertThat(events).isEmpty()
        }

        @Test
        @DisplayName("배달 수행 중에는 퇴근할 수 없다")
        fun `배달 수행 중에는 퇴근할 수 없다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            assertThatThrownBy { robot.endDuty() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageMatching(".*(퇴근할 수 없는 상태입니다|배달 수행 중에는 퇴근할 수 없습니다).*")
        }

        @Test
        @DisplayName("OFF_DUTY 상태에서 퇴근하면 예외가 발생한다")
        fun `OFF_DUTY 상태에서 퇴근하면 예외가 발생한다`() {
            val robot = createRobot()

            assertThatThrownBy { robot.endDuty() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("퇴근할 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("배달 할당 테스트")
    inner class AssignDeliveryTest {
        @Test
        @DisplayName("READY 상태에서 배달을 할당받을 수 있다")
        fun `READY 상태에서 배달을 할당받을 수 있다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val deliveryId = DeliveryId(1L)
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)

            robot.assignDelivery(deliveryId, pickupLocation)

            assertThat(robot.status).isEqualTo(RobotStatus.BUSY)
            assertThat(robot.currentDeliveryId).isEqualTo(deliveryId)
        }

        @Test
        @DisplayName("배달 할당 시 RobotDestinationChangedEvent가 발생한다")
        fun `배달 할당 시 RobotDestinationChangedEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val deliveryId = DeliveryId(1L)
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)

            robot.assignDelivery(deliveryId, pickupLocation)
            val events = robot.pullDomainEvents()

            assertThat(events).hasSize(1)
            val destinationChangedEvent = events[0] as RobotDestinationChangedEvent
            assertThat(destinationChangedEvent.destination).isEqualTo(pickupLocation)
        }

        @Test
        @DisplayName("OFF_DUTY 상태에서 배달을 할당받으면 예외가 발생한다")
        fun `OFF_DUTY 상태에서 배달을 할당받으면 예외가 발생한다`() {
            val robot = createRobot()
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)

            assertThatThrownBy { robot.assignDelivery(DeliveryId(1L), pickupLocation) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달을 받을 수 없는 상태입니다")
        }

        @Test
        @DisplayName("이미 배달 수행 중이면 새 배달을 할당받을 수 없다")
        fun `이미 배달 수행 중이면 새 배달을 할당받을 수 없다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)

            assertThatThrownBy { robot.assignDelivery(DeliveryId(2L), pickupLocation) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageMatching(".*(배달을 받을 수 없는 상태입니다|이미 다른 배달을 수행 중입니다).*")
        }
    }

    @Nested
    @DisplayName("배달 완료 테스트")
    inner class CompleteDeliveryTest {
        @Test
        @DisplayName("BUSY 상태에서 배달을 완료할 수 있다")
        fun `BUSY 상태에서 배달을 완료할 수 있다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            robot.completeDelivery()

            assertThat(robot.status).isEqualTo(RobotStatus.READY)
            assertThat(robot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("배달 완료 시 destination과 drivingStatus가 초기화된다")
        fun `배달 완료 시 destination과 drivingStatus가 초기화된다`() {
            val destination = Location(latitude = 37.5000, longitude = 127.0000)
            val robot = createRobot(
                status = RobotStatus.BUSY,
                currentDeliveryId = DeliveryId(1L),
                drivingStatus = RobotDrivingStatus.APPROACHING,
                destination = destination,
            )

            robot.completeDelivery()

            assertThat(robot.destination).isNull()
            assertThat(robot.drivingStatus).isEqualTo(RobotDrivingStatus.ARRIVED)
        }

        @Test
        @DisplayName("배달 완료 시 RobotBecameAvailableEvent가 발생한다")
        fun `배달 완료 시 RobotBecameAvailableEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            robot.completeDelivery()
            val events = robot.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(RobotBecameAvailableEvent::class.java)
        }

        @Test
        @DisplayName("READY 상태에서 배달 완료하면 예외가 발생한다")
        fun `READY 상태에서 배달 완료하면 예외가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)

            assertThatThrownBy { robot.completeDelivery() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 수행 중이 아닙니다")
        }
    }

    @Nested
    @DisplayName("배차 취소 테스트")
    inner class UnassignDeliveryTest {
        @Test
        @DisplayName("BUSY 상태에서 배차를 취소할 수 있다")
        fun `BUSY 상태에서 배차를 취소할 수 있다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            robot.unassignDelivery()

            assertThat(robot.status).isEqualTo(RobotStatus.READY)
            assertThat(robot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("배차 취소 시 destination과 drivingStatus가 초기화된다")
        fun `배차 취소 시 destination과 drivingStatus가 초기화된다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            robot.assignDelivery(DeliveryId(1L), pickupLocation)

            robot.unassignDelivery()

            assertThat(robot.destination).isNull()
            assertThat(robot.drivingStatus).isEqualTo(RobotDrivingStatus.ARRIVED)
        }

        @Test
        @DisplayName("배차 취소 시 RobotBecameAvailableEvent가 발생한다")
        fun `배차 취소 시 RobotBecameAvailableEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            robot.unassignDelivery()
            val events = robot.pullDomainEvents()

            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(RobotBecameAvailableEvent::class.java)
        }

        @Test
        @DisplayName("READY 상태에서 배차 취소하면 예외가 발생한다")
        fun `READY 상태에서 배차 취소하면 예외가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)

            assertThatThrownBy { robot.unassignDelivery() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 수행 중이 아닙니다")
        }

        @Test
        @DisplayName("할당된 배달이 없으면 예외가 발생한다")
        fun `할당된 배달이 없으면 예외가 발생한다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = null)

            assertThatThrownBy { robot.unassignDelivery() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("할당된 배달이 없습니다")
        }
    }

    @Nested
    @DisplayName("주행 상태 업데이트 테스트")
    inner class UpdateDrivingStatusTest {
        private val destination = Location(latitude = 37.5000, longitude = 127.0000)

        @Test
        @DisplayName("ON_GOING 상태에서 목적지 50m 이내 진입 시 APPROACHING으로 변경되고 RobotApproachingEvent가 발행된다")
        fun `ON_GOING 상태에서 목적지 50m 이내 진입 시 APPROACHING으로 변경되고 RobotApproachingEvent가 발행된다`() {
            val robot = createRobot(
                status = RobotStatus.BUSY,
                drivingStatus = RobotDrivingStatus.ON_GOING,
                currentDeliveryId = DeliveryId(1L),
                destination = destination,
            )
            val locationWithin50m = Location(latitude = 37.5003, longitude = 127.0000)

            robot.updateDrivingStatus(locationWithin50m)
            val events = robot.pullDomainEvents()

            assertThat(robot.drivingStatus).isEqualTo(RobotDrivingStatus.APPROACHING)
            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(RobotApproachingEvent::class.java)
        }

        @Test
        @DisplayName("APPROACHING 상태에서 목적지 5m 이내 진입 시 ARRIVED로 변경되고 RobotArrivedEvent가 발행된다")
        fun `APPROACHING 상태에서 목적지 5m 이내 진입 시 ARRIVED로 변경되고 RobotArrivedEvent가 발행된다`() {
            val robot = createRobot(
                status = RobotStatus.BUSY,
                drivingStatus = RobotDrivingStatus.APPROACHING,
                currentDeliveryId = DeliveryId(1L),
                destination = destination,
            )
            val locationWithin5m = Location(latitude = 37.50003, longitude = 127.0000)

            robot.updateDrivingStatus(locationWithin5m)
            val events = robot.pullDomainEvents()

            assertThat(robot.drivingStatus).isEqualTo(RobotDrivingStatus.ARRIVED)
            assertThat(events).hasSize(1)
            assertThat(events[0]).isInstanceOf(RobotArrivedEvent::class.java)
        }

        @Test
        @DisplayName("ON_GOING 상태에서 목적지 5m 이내 진입 시 ARRIVED로 변경되고 두 이벤트가 모두 발행된다")
        fun `ON_GOING 상태에서 목적지 5m 이내 진입 시 ARRIVED로 변경되고 두 이벤트가 모두 발행된다`() {
            val robot = createRobot(
                status = RobotStatus.BUSY,
                drivingStatus = RobotDrivingStatus.ON_GOING,
                currentDeliveryId = DeliveryId(1L),
                destination = destination,
            )
            val locationWithin5m = Location(latitude = 37.50003, longitude = 127.0000)

            robot.updateDrivingStatus(locationWithin5m)
            val events = robot.pullDomainEvents()

            assertThat(robot.drivingStatus).isEqualTo(RobotDrivingStatus.ARRIVED)
            assertThat(events).hasSize(2)
            assertThat(events[0]).isInstanceOf(RobotApproachingEvent::class.java)
            assertThat(events[1]).isInstanceOf(RobotArrivedEvent::class.java)
        }

        @Test
        @DisplayName("목적지가 없으면 상태가 변경되지 않는다")
        fun `목적지가 없으면 상태가 변경되지 않는다`() {
            val robot = createRobot(
                status = RobotStatus.BUSY,
                drivingStatus = RobotDrivingStatus.ON_GOING,
                currentDeliveryId = DeliveryId(1L),
                destination = null,
            )
            val newLocation = Location(latitude = 37.5000, longitude = 127.0000)

            robot.updateDrivingStatus(newLocation)
            val events = robot.pullDomainEvents()

            assertThat(robot.drivingStatus).isEqualTo(RobotDrivingStatus.ON_GOING)
            assertThat(events).isEmpty()
        }

        @Test
        @DisplayName("ON_GOING 상태에서 목적지 50m 초과 시 상태가 유지된다")
        fun `ON_GOING 상태에서 목적지 50m 초과 시 상태가 유지된다`() {
            val robot = createRobot(
                status = RobotStatus.BUSY,
                drivingStatus = RobotDrivingStatus.ON_GOING,
                currentDeliveryId = DeliveryId(1L),
                destination = destination,
            )
            val locationBeyond50m = Location(latitude = 37.501, longitude = 127.0000)

            robot.updateDrivingStatus(locationBeyond50m)
            val events = robot.pullDomainEvents()

            assertThat(robot.drivingStatus).isEqualTo(RobotDrivingStatus.ON_GOING)
            assertThat(events).isEmpty()
        }
    }

    @Nested
    @DisplayName("배달 가용성 테스트")
    inner class AvailabilityForDeliveryTest {
        @Test
        @DisplayName("READY 상태이고 배달이 없으면 배달 가능하다")
        fun `READY 상태이고 배달이 없으면 배달 가능하다`() {
            val robot = createRobot(status = RobotStatus.READY)

            assertThat(robot.isAvailableForDelivery()).isTrue()
        }

        @Test
        @DisplayName("BUSY 상태이면 배달 불가능하다")
        fun `BUSY 상태이면 배달 불가능하다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            assertThat(robot.isAvailableForDelivery()).isFalse()
        }

        @Test
        @DisplayName("OFF_DUTY 상태이면 배달 불가능하다")
        fun `OFF_DUTY 상태이면 배달 불가능하다`() {
            val robot = createRobot()

            assertThat(robot.isAvailableForDelivery()).isFalse()
        }

        @Test
        @DisplayName("현재 배달이 할당되어 있으면 배달 불가능하다")
        fun `현재 배달이 할당되어 있으면 배달 불가능하다`() {
            val robot = createRobot(status = RobotStatus.READY, currentDeliveryId = DeliveryId(1L))

            assertThat(robot.isAvailableForDelivery()).isFalse()
        }
    }

    @Nested
    @DisplayName("목적지 이동 테스트")
    inner class NavigateToTest {
        @Test
        @DisplayName("BUSY 상태에서 목적지를 설정할 수 있다")
        fun `BUSY 상태에서 목적지를 설정할 수 있다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))
            val destination = Location(latitude = 37.5000, longitude = 127.0000)

            robot.navigateTo(destination)

            assertThat(robot.destination).isEqualTo(destination)
        }

        @Test
        @DisplayName("목적지 설정 시 RobotDestinationChangedEvent가 발생한다")
        fun `목적지 설정 시 RobotDestinationChangedEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))
            val destination = Location(latitude = 37.5000, longitude = 127.0000)

            robot.navigateTo(destination)
            val events = robot.pullDomainEvents()

            assertThat(events).hasSize(1)
            val event = events[0] as RobotDestinationChangedEvent
            assertThat(event.robotId).isEqualTo(robot.getRobotId())
            assertThat(event.destination).isEqualTo(destination)
        }

        @Test
        @DisplayName("READY 상태에서 목적지 설정하면 예외가 발생한다")
        fun `READY 상태에서 목적지 설정하면 예외가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val destination = Location(latitude = 37.5000, longitude = 127.0000)

            assertThatThrownBy { robot.navigateTo(destination) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 수행 중이 아닙니다")
        }

        @Test
        @DisplayName("OFF_DUTY 상태에서 목적지 설정하면 예외가 발생한다")
        fun `OFF_DUTY 상태에서 목적지 설정하면 예외가 발생한다`() {
            val robot = createRobot()
            val destination = Location(latitude = 37.5000, longitude = 127.0000)

            assertThatThrownBy { robot.navigateTo(destination) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 수행 중이 아닙니다")
        }
    }
}
