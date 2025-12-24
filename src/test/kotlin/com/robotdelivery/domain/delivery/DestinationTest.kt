package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.Location
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Destination 테스트")
class DestinationTest {
    private val validLocation = Location(latitude = 37.5665, longitude = 126.9780)

    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("유효한 주소로 Destination을 생성할 수 있다")
        fun `유효한 주소로 Destination을 생성할 수 있다`() {
            val destination =
                Destination(
                    address = "서울시 중구 세종대로 110",
                    addressDetail = "시청역 1번 출구",
                    location = validLocation,
                )

            assertThat(destination.address).isEqualTo("서울시 중구 세종대로 110")
            assertThat(destination.addressDetail).isEqualTo("시청역 1번 출구")
            assertThat(destination.location).isEqualTo(validLocation)
        }

        @Test
        @DisplayName("상세 주소 없이 Destination을 생성할 수 있다")
        fun `상세 주소 없이 Destination을 생성할 수 있다`() {
            val destination =
                Destination(
                    address = "서울시 중구 세종대로 110",
                    location = validLocation,
                )

            assertThat(destination.address).isEqualTo("서울시 중구 세종대로 110")
            assertThat(destination.addressDetail).isNull()
        }

        @Test
        @DisplayName("빈 주소로 Destination을 생성하면 예외가 발생한다")
        fun `빈 주소로 Destination을 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                Destination(
                    address = "",
                    location = validLocation,
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("주소는 비어있을 수 없습니다.")
        }

        @Test
        @DisplayName("공백만 있는 주소로 Destination을 생성하면 예외가 발생한다")
        fun `공백만 있는 주소로 Destination을 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                Destination(
                    address = "   ",
                    location = validLocation,
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("주소는 비어있을 수 없습니다.")
        }
    }

    @Nested
    @DisplayName("동등성 테스트")
    inner class EqualityTest {
        @Test
        @DisplayName("같은 값을 가진 Destination은 동등하다")
        fun `같은 값을 가진 Destination은 동등하다`() {
            val destination1 =
                Destination(
                    address = "서울시 중구 세종대로 110",
                    addressDetail = "시청역 1번 출구",
                    location = validLocation,
                )
            val destination2 =
                Destination(
                    address = "서울시 중구 세종대로 110",
                    addressDetail = "시청역 1번 출구",
                    location = validLocation,
                )

            assertThat(destination1).isEqualTo(destination2)
        }

        @Test
        @DisplayName("다른 주소를 가진 Destination은 동등하지 않다")
        fun `다른 주소를 가진 Destination은 동등하지 않다`() {
            val destination1 =
                Destination(
                    address = "서울시 중구 세종대로 110",
                    location = validLocation,
                )
            val destination2 =
                Destination(
                    address = "서울시 강남구 테헤란로 1",
                    location = validLocation,
                )

            assertThat(destination1).isNotEqualTo(destination2)
        }
    }
}
