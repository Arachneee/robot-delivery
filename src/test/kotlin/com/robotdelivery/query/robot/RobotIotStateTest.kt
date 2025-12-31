package com.robotdelivery.query.robot

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.robot.RobotIotState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RobotIotState 테스트")
class RobotIotStateTest {
    private val defaultLocation = Location(latitude = 37.5665, longitude = 126.9780)

    private fun createIotState(
        robotId: RobotId = RobotId(1L),
        location: Location = defaultLocation,
        battery: Int = 100,
        doorOpen: Boolean = false,
        loadWeight: Double = 0.0,
    ) = RobotIotState(
        robotId = robotId,
        location = location,
        battery = battery,
        doorOpen = doorOpen,
        loadWeight = loadWeight,
    )

    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("유효한 값으로 생성할 수 있다")
        fun `유효한 값으로 생성할 수 있다`() {
            val iotState =
                createIotState(
                    battery = 80,
                    doorOpen = true,
                    loadWeight = 5.5,
                )

            assertThat(iotState.battery).isEqualTo(80)
            assertThat(iotState.doorOpen).isTrue()
            assertThat(iotState.loadWeight).isEqualTo(5.5)
        }

        @Test
        @DisplayName("배터리가 0 미만이면 예외가 발생한다")
        fun `배터리가 0 미만이면 예외가 발생한다`() {
            assertThatThrownBy { createIotState(battery = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("배터리는 0에서 100 사이여야 합니다")
        }

        @Test
        @DisplayName("배터리가 100 초과이면 예외가 발생한다")
        fun `배터리가 100 초과이면 예외가 발생한다`() {
            assertThatThrownBy { createIotState(battery = 101) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("배터리는 0에서 100 사이여야 합니다")
        }

        @Test
        @DisplayName("적재 무게가 음수이면 예외가 발생한다")
        fun `적재 무게가 음수이면 예외가 발생한다`() {
            assertThatThrownBy { createIotState(loadWeight = -1.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("적재 무게는 0 이상이어야 합니다")
        }
    }

    @Nested
    @DisplayName("배터리 충분 여부 테스트")
    inner class HasSufficientBatteryTest {
        @Test
        @DisplayName("배터리가 최소값 이상이면 true를 반환한다")
        fun `배터리가 최소값 이상이면 true를 반환한다`() {
            val iotState = createIotState(battery = 50)

            assertThat(iotState.hasSufficientBattery(20)).isTrue()
        }

        @Test
        @DisplayName("배터리가 최소값 미만이면 false를 반환한다")
        fun `배터리가 최소값 미만이면 false를 반환한다`() {
            val iotState = createIotState(battery = 19)

            assertThat(iotState.hasSufficientBattery(20)).isFalse()
        }

        @Test
        @DisplayName("배터리가 정확히 최소값이면 true를 반환한다")
        fun `배터리가 정확히 최소값이면 true를 반환한다`() {
            val iotState = createIotState(battery = 20)

            assertThat(iotState.hasSufficientBattery(20)).isTrue()
        }

        @Test
        @DisplayName("기본 최소값은 20이다")
        fun `기본 최소값은 20이다`() {
            val iotState = createIotState(battery = 20)

            assertThat(iotState.hasSufficientBattery()).isTrue()
        }
    }

    @Nested
    @DisplayName("거리 계산 테스트")
    inner class DistanceToTest {
        @Test
        @DisplayName("다른 위치까지의 거리를 계산할 수 있다")
        fun `다른 위치까지의 거리를 계산할 수 있다`() {
            val iotState = createIotState(location = Location(37.5000, 127.0000))
            val destination = Location(37.5000, 127.0001)

            val distance = iotState.distanceTo(destination)

            assertThat(distance).isGreaterThan(0.0)
        }

        @Test
        @DisplayName("같은 위치면 거리가 0이다")
        fun `같은 위치면 거리가 0이다`() {
            val location = Location(37.5000, 127.0000)
            val iotState = createIotState(location = location)

            val distance = iotState.distanceTo(location)

            assertThat(distance).isEqualTo(0.0)
        }
    }
}
