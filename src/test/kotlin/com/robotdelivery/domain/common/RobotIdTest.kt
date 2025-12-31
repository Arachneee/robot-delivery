package com.robotdelivery.domain.common

import com.robotdelivery.domain.common.vo.RobotId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RobotId 테스트")
class RobotIdTest {
    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("양수 값으로 RobotId를 생성할 수 있다")
        fun `양수 값으로 RobotId를 생성할 수 있다`() {
            val robotId = RobotId(1L)

            assertThat(robotId.value).isEqualTo(1L)
        }

        @Test
        @DisplayName("큰 양수 값으로 RobotId를 생성할 수 있다")
        fun `큰 양수 값으로 RobotId를 생성할 수 있다`() {
            val robotId = RobotId(Long.MAX_VALUE)

            assertThat(robotId.value).isEqualTo(Long.MAX_VALUE)
        }

        @Test
        @DisplayName("0 이하의 값으로 RobotId를 생성하면 예외가 발생한다")
        fun `0 이하의 값으로 RobotId를 생성하면 예외가 발생한다`() {
            assertThatThrownBy { RobotId(0L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("RobotId는 0보다 커야 합니다")

            assertThatThrownBy { RobotId(-1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("RobotId는 0보다 커야 합니다")
        }

        @Test
        @DisplayName("음수 값으로 RobotId를 생성하면 예외가 발생한다")
        fun `음수 값으로 RobotId를 생성하면 예외가 발생한다`() {
            assertThatThrownBy { RobotId(-100L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("RobotId는 0보다 커야 합니다: -100")
        }
    }

    @Nested
    @DisplayName("동등성 테스트")
    inner class EqualityTest {
        @Test
        @DisplayName("같은 값을 가진 RobotId는 동등하다")
        fun `같은 값을 가진 RobotId는 동등하다`() {
            val id1 = RobotId(1L)
            val id2 = RobotId(1L)

            assertThat(id1).isEqualTo(id2)
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
        }

        @Test
        @DisplayName("다른 값을 가진 RobotId는 동등하지 않다")
        fun `다른 값을 가진 RobotId는 동등하지 않다`() {
            val id1 = RobotId(1L)
            val id2 = RobotId(2L)

            assertThat(id1).isNotEqualTo(id2)
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    inner class ToStringTest {
        @Test
        @DisplayName("toString은 값만 반환한다")
        fun `toString은 값만 반환한다`() {
            val robotId = RobotId(456L)

            assertThat(robotId.toString()).isEqualTo("456")
        }
    }
}
