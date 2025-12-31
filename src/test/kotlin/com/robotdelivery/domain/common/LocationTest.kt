package com.robotdelivery.domain.common

import com.robotdelivery.domain.common.vo.Location
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Location 테스트")
class LocationTest {
    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("유효한 좌표로 Location을 생성할 수 있다")
        fun `유효한 좌표로 Location을 생성할 수 있다`() {
            val location = Location(latitude = 37.5665, longitude = 126.9780)

            assertThat(location.latitude).isEqualTo(37.5665)
            assertThat(location.longitude).isEqualTo(126.9780)
        }

        @Test
        @DisplayName("위도가 -90보다 작으면 예외가 발생한다")
        fun `위도가 -90보다 작으면 예외가 발생한다`() {
            assertThatThrownBy { Location(latitude = -91.0, longitude = 0.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("위도는 -90에서 90 사이여야 합니다.")
        }

        @Test
        @DisplayName("위도가 90보다 크면 예외가 발생한다")
        fun `위도가 90보다 크면 예외가 발생한다`() {
            assertThatThrownBy { Location(latitude = 91.0, longitude = 0.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("위도는 -90에서 90 사이여야 합니다.")
        }

        @Test
        @DisplayName("경도가 -180보다 작으면 예외가 발생한다")
        fun `경도가 -180보다 작으면 예외가 발생한다`() {
            assertThatThrownBy { Location(latitude = 0.0, longitude = -181.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("경도는 -180에서 180 사이여야 합니다.")
        }

        @Test
        @DisplayName("경도가 180보다 크면 예외가 발생한다")
        fun `경도가 180보다 크면 예외가 발생한다`() {
            assertThatThrownBy { Location(latitude = 0.0, longitude = 181.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("경도는 -180에서 180 사이여야 합니다.")
        }

        @Test
        @DisplayName("경계값 좌표로 Location을 생성할 수 있다")
        fun `경계값 좌표로 Location을 생성할 수 있다`() {
            assertThatCode { Location(latitude = -90.0, longitude = -180.0) }.doesNotThrowAnyException()
            assertThatCode { Location(latitude = 90.0, longitude = 180.0) }.doesNotThrowAnyException()
            assertThatCode { Location(latitude = 0.0, longitude = 0.0) }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("거리 계산 테스트")
    inner class DistanceCalculationTest {
        @Test
        @DisplayName("같은 위치 간의 거리는 0이다")
        fun `같은 위치 간의 거리는 0이다`() {
            val location = Location(latitude = 37.5665, longitude = 126.9780)
            val distance = location.distanceTo(location)

            assertThat(distance).isCloseTo(0.0, within(0.001))
        }

        @Test
        @DisplayName("서울시청과 강남역 간의 거리를 계산할 수 있다")
        fun `서울시청과 강남역 간의 거리를 계산할 수 있다`() {
            val seoulCityHall = Location(latitude = 37.5665, longitude = 126.9780)
            val gangnamStation = Location(latitude = 37.4979, longitude = 127.0276)

            val distance = seoulCityHall.distanceTo(gangnamStation)

            assertThat(distance).isBetween(8000.0, 10000.0)
        }

        @Test
        @DisplayName("거리 계산은 대칭적이다")
        fun `거리 계산은 대칭적이다`() {
            val locationA = Location(latitude = 37.5665, longitude = 126.9780)
            val locationB = Location(latitude = 35.1796, longitude = 129.0756)

            val distanceAB = locationA.distanceTo(locationB)
            val distanceBA = locationB.distanceTo(locationA)

            assertThat(distanceAB).isCloseTo(distanceBA, within(0.001))
        }

        @Test
        @DisplayName("가까운 두 지점 간의 거리를 정확하게 계산한다")
        fun `가까운 두 지점 간의 거리를 정확하게 계산한다`() {
            val location1 = Location(latitude = 37.5000, longitude = 127.0000)
            val location2 = Location(latitude = 37.5001, longitude = 127.0001)

            val distance = location1.distanceTo(location2)

            assertThat(distance).isGreaterThan(0.0).isLessThan(20.0)
        }
    }
}
