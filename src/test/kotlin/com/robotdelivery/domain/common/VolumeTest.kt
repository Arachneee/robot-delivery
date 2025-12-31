package com.robotdelivery.domain.common

import com.robotdelivery.domain.common.vo.Volume
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Volume 테스트")
class VolumeTest {
    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("양수 값으로 Volume을 생성할 수 있다")
        fun `양수 값으로 Volume을 생성할 수 있다`() {
            val volume = Volume(10.0)

            assertThat(volume.value).isEqualTo(10.0)
        }

        @Test
        @DisplayName("0 값으로 Volume을 생성할 수 있다")
        fun `0 값으로 Volume을 생성할 수 있다`() {
            val volume = Volume(0.0)

            assertThat(volume.value).isEqualTo(0.0)
        }

        @Test
        @DisplayName("음수 값으로 Volume을 생성하면 예외가 발생한다")
        fun `음수 값으로 Volume을 생성하면 예외가 발생한다`() {
            assertThatThrownBy { Volume(-1.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Volume은 0 이상이어야 합니다")
        }
    }

    @Nested
    @DisplayName("연산 테스트")
    inner class OperationTest {
        @Test
        @DisplayName("두 Volume을 더할 수 있다")
        fun `두 Volume을 더할 수 있다`() {
            val volume1 = Volume(10.0)
            val volume2 = Volume(20.0)

            val result = volume1 + volume2

            assertThat(result.value).isEqualTo(30.0)
        }

        @Test
        @DisplayName("Volume에 수량을 곱할 수 있다")
        fun `Volume에 수량을 곱할 수 있다`() {
            val volume = Volume(10.0)

            val result = volume * 3

            assertThat(result.value).isEqualTo(30.0)
        }

        @Test
        @DisplayName("ZERO 상수는 0 값을 가진다")
        fun `ZERO 상수는 0 값을 가진다`() {
            assertThat(Volume.ZERO.value).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("비교 테스트")
    inner class CompareTest {
        @Test
        @DisplayName("더 큰 Volume은 양수를 반환한다")
        fun `더 큰 Volume은 양수를 반환한다`() {
            val volume1 = Volume(20.0)
            val volume2 = Volume(10.0)

            assertThat(volume1 > volume2).isTrue()
        }

        @Test
        @DisplayName("더 작은 Volume은 음수를 반환한다")
        fun `더 작은 Volume은 음수를 반환한다`() {
            val volume1 = Volume(10.0)
            val volume2 = Volume(20.0)

            assertThat(volume1 < volume2).isTrue()
        }

        @Test
        @DisplayName("같은 Volume은 0을 반환한다")
        fun `같은 Volume은 0을 반환한다`() {
            val volume1 = Volume(10.0)
            val volume2 = Volume(10.0)

            assertThat(volume1 >= volume2).isTrue()
            assertThat(volume1 <= volume2).isTrue()
        }
    }
}
