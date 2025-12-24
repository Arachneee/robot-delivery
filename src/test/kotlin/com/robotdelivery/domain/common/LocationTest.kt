package com.robotdelivery.domain.common

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Location 테스트")
class LocationTest {
    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("유효한 좌표로 Location을 생성할 수 있다")
        fun `유효한 좌표로 Location을 생성할 수 있다`() {
            val location = Location(latitude = 37.5665, longitude = 126.9780)

            assertEquals(37.5665, location.latitude)
            assertEquals(126.9780, location.longitude)
        }

        @Test
        @DisplayName("위도가 -90보다 작으면 예외가 발생한다")
        fun `위도가 -90보다 작으면 예외가 발생한다`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Location(latitude = -91.0, longitude = 0.0)
                }
            assertEquals("위도는 -90에서 90 사이여야 합니다.", exception.message)
        }

        @Test
        @DisplayName("위도가 90보다 크면 예외가 발생한다")
        fun `위도가 90보다 크면 예외가 발생한다`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Location(latitude = 91.0, longitude = 0.0)
                }
            assertEquals("위도는 -90에서 90 사이여야 합니다.", exception.message)
        }

        @Test
        @DisplayName("경도가 -180보다 작으면 예외가 발생한다")
        fun `경도가 -180보다 작으면 예외가 발생한다`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Location(latitude = 0.0, longitude = -181.0)
                }
            assertEquals("경도는 -180에서 180 사이여야 합니다.", exception.message)
        }

        @Test
        @DisplayName("경도가 180보다 크면 예외가 발생한다")
        fun `경도가 180보다 크면 예외가 발생한다`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Location(latitude = 0.0, longitude = 181.0)
                }
            assertEquals("경도는 -180에서 180 사이여야 합니다.", exception.message)
        }

        @Test
        @DisplayName("경계값 좌표로 Location을 생성할 수 있다")
        fun `경계값 좌표로 Location을 생성할 수 있다`() {
            assertDoesNotThrow { Location(latitude = -90.0, longitude = -180.0) }
            assertDoesNotThrow { Location(latitude = 90.0, longitude = 180.0) }
            assertDoesNotThrow { Location(latitude = 0.0, longitude = 0.0) }
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

            assertEquals(0.0, distance, 0.001)
        }

        @Test
        @DisplayName("서울시청과 강남역 간의 거리를 계산할 수 있다")
        fun `서울시청과 강남역 간의 거리를 계산할 수 있다`() {
            val seoulCityHall = Location(latitude = 37.5665, longitude = 126.9780)
            val gangnamStation = Location(latitude = 37.4979, longitude = 127.0276)

            val distance = seoulCityHall.distanceTo(gangnamStation)

            // 약 9km 정도 거리
            assertTrue(distance > 8000 && distance < 10000)
        }

        @Test
        @DisplayName("거리 계산은 대칭적이다")
        fun `거리 계산은 대칭적이다`() {
            val locationA = Location(latitude = 37.5665, longitude = 126.9780)
            val locationB = Location(latitude = 35.1796, longitude = 129.0756)

            val distanceAB = locationA.distanceTo(locationB)
            val distanceBA = locationB.distanceTo(locationA)

            assertEquals(distanceAB, distanceBA, 0.001)
        }

        @Test
        @DisplayName("가까운 두 지점 간의 거리를 정확하게 계산한다")
        fun `가까운 두 지점 간의 거리를 정확하게 계산한다`() {
            val location1 = Location(latitude = 37.5000, longitude = 127.0000)
            val location2 = Location(latitude = 37.5001, longitude = 127.0001)

            val distance = location1.distanceTo(location2)

            // 매우 가까운 거리 (약 13m 정도)
            assertTrue(distance > 0 && distance < 20)
        }
    }
}
