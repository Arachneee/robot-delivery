package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.robot.event.RobotApproachingEvent
import com.robotdelivery.domain.robot.event.RobotArrivedEvent
import com.robotdelivery.domain.robot.vo.RobotDrivingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RobotDrivingStatus 테스트")
class RobotDrivingStatusTest {
    private val robotId = RobotId(1L)
    private val destination = Location(37.5000, 127.0000)

    @Nested
    @DisplayName("ON_GOING 상태 테스트")
    inner class OnGoingTest {
        @Test
        @DisplayName("nextStatus는 APPROACHING이다")
        fun `nextStatus는 APPROACHING이다`() {
            assertThat(RobotDrivingStatus.ON_GOING.nextStatus).isEqualTo(RobotDrivingStatus.APPROACHING)
        }

        @Test
        @DisplayName("thresholdMeter는 1500.0이다")
        fun `thresholdMeter는 1500이다`() {
            assertThat(RobotDrivingStatus.ON_GOING.thresholdMeter).isEqualTo(1500.0)
        }

        @Test
        @DisplayName("createEvent는 null을 반환한다")
        fun `createEvent는 null을 반환한다`() {
            val event = RobotDrivingStatus.ON_GOING.createEvent(robotId, destination)
            assertThat(event).isNull()
        }

        @Test
        @DisplayName("거리가 50m 이하면 다음 상태로 전이 가능하다")
        fun `거리가 50m 이하면 다음 상태로 전이 가능하다`() {
            assertThat(RobotDrivingStatus.ON_GOING.isAbleToNextStatus(50.0)).isTrue()
            assertThat(RobotDrivingStatus.ON_GOING.isAbleToNextStatus(49.0)).isTrue()
        }

        @Test
        @DisplayName("거리가 50m 초과면 다음 상태로 전이 불가하다")
        fun `거리가 50m 초과면 다음 상태로 전이 불가하다`() {
            assertThat(RobotDrivingStatus.ON_GOING.isAbleToNextStatus(51.0)).isFalse()
            assertThat(RobotDrivingStatus.ON_GOING.isAbleToNextStatus(100.0)).isFalse()
        }
    }

    @Nested
    @DisplayName("APPROACHING 상태 테스트")
    inner class ApproachingTest {
        @Test
        @DisplayName("nextStatus는 ARRIVED이다")
        fun `nextStatus는 ARRIVED이다`() {
            assertThat(RobotDrivingStatus.APPROACHING.nextStatus).isEqualTo(RobotDrivingStatus.ARRIVED)
        }

        @Test
        @DisplayName("thresholdMeter는 50.0이다")
        fun `thresholdMeter는 50이다`() {
            assertThat(RobotDrivingStatus.APPROACHING.thresholdMeter).isEqualTo(50.0)
        }

        @Test
        @DisplayName("createEvent는 RobotApproachingEvent를 반환한다")
        fun `createEvent는 RobotApproachingEvent를 반환한다`() {
            val event = RobotDrivingStatus.APPROACHING.createEvent(robotId, destination)

            assertThat(event).isNotNull
            assertThat(event).isInstanceOf(RobotApproachingEvent::class.java)
            val approachingEvent = event as RobotApproachingEvent
            assertThat(approachingEvent.robotId).isEqualTo(robotId)
            assertThat(approachingEvent.destination).isEqualTo(destination)
        }

        @Test
        @DisplayName("거리가 5m 이하면 다음 상태로 전이 가능하다")
        fun `거리가 5m 이하면 다음 상태로 전이 가능하다`() {
            assertThat(RobotDrivingStatus.APPROACHING.isAbleToNextStatus(5.0)).isTrue()
            assertThat(RobotDrivingStatus.APPROACHING.isAbleToNextStatus(4.0)).isTrue()
        }

        @Test
        @DisplayName("거리가 5m 초과면 다음 상태로 전이 불가하다")
        fun `거리가 5m 초과면 다음 상태로 전이 불가하다`() {
            assertThat(RobotDrivingStatus.APPROACHING.isAbleToNextStatus(6.0)).isFalse()
            assertThat(RobotDrivingStatus.APPROACHING.isAbleToNextStatus(10.0)).isFalse()
        }
    }

    @Nested
    @DisplayName("ARRIVED 상태 테스트")
    inner class ArrivedTest {
        @Test
        @DisplayName("nextStatus는 null이다")
        fun `nextStatus는 null이다`() {
            assertThat(RobotDrivingStatus.ARRIVED.nextStatus).isNull()
        }

        @Test
        @DisplayName("thresholdMeter는 5.0이다")
        fun `thresholdMeter는 5이다`() {
            assertThat(RobotDrivingStatus.ARRIVED.thresholdMeter).isEqualTo(5.0)
        }

        @Test
        @DisplayName("createEvent는 RobotArrivedEvent를 반환한다")
        fun `createEvent는 RobotArrivedEvent를 반환한다`() {
            val event = RobotDrivingStatus.ARRIVED.createEvent(robotId, destination)

            assertThat(event).isNotNull
            assertThat(event).isInstanceOf(RobotArrivedEvent::class.java)
            val arrivedEvent = event as RobotArrivedEvent
            assertThat(arrivedEvent.robotId).isEqualTo(robotId)
            assertThat(arrivedEvent.destination).isEqualTo(destination)
        }

        @Test
        @DisplayName("nextStatus가 null이므로 다음 상태로 전이 불가하다")
        fun `nextStatus가 null이므로 다음 상태로 전이 불가하다`() {
            assertThat(RobotDrivingStatus.ARRIVED.isAbleToNextStatus(0.0)).isFalse()
            assertThat(RobotDrivingStatus.ARRIVED.isAbleToNextStatus(1.0)).isFalse()
        }
    }

    @Nested
    @DisplayName("상태 전이 시나리오 테스트")
    inner class TransitionScenarioTest {
        @Test
        @DisplayName("ON_GOING에서 APPROACHING으로 거리가 50m 이하일 때 전이 가능")
        fun `ON_GOING에서 APPROACHING으로 거리가 50m 이하일 때 전이 가능`() {
            val status = RobotDrivingStatus.ON_GOING
            val distanceTo = 45.0

            assertThat(status.isAbleToNextStatus(distanceTo)).isTrue()
            assertThat(status.nextStatus).isEqualTo(RobotDrivingStatus.APPROACHING)
        }

        @Test
        @DisplayName("APPROACHING에서 ARRIVED로 거리가 5m 이하일 때 전이 가능")
        fun `APPROACHING에서 ARRIVED로 거리가 5m 이하일 때 전이 가능`() {
            val status = RobotDrivingStatus.APPROACHING
            val distanceTo = 3.0

            assertThat(status.isAbleToNextStatus(distanceTo)).isTrue()
            assertThat(status.nextStatus).isEqualTo(RobotDrivingStatus.ARRIVED)
        }

        @Test
        @DisplayName("경계값 테스트 - 정확히 threshold 값일 때")
        fun `경계값 테스트 - 정확히 threshold 값일 때`() {
            assertThat(RobotDrivingStatus.ON_GOING.isAbleToNextStatus(50.0)).isTrue()
            assertThat(RobotDrivingStatus.APPROACHING.isAbleToNextStatus(5.0)).isTrue()
        }
    }
}
