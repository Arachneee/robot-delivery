package com.robotdelivery.domain.robot

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RobotStatus 테스트")
class RobotStatusTest {
    @Nested
    @DisplayName("상태 전이 테스트")
    inner class TransitionTest {
        @Test
        @DisplayName("OFF_DUTY 상태에서 READY로 전이할 수 있다")
        fun `OFF_DUTY 상태에서 READY로 전이할 수 있다`() {
            assertTrue(RobotStatus.OFF_DUTY.canTransitionTo(RobotStatus.READY))
        }

        @Test
        @DisplayName("OFF_DUTY 상태에서 BUSY로 직접 전이할 수 없다")
        fun `OFF_DUTY 상태에서 BUSY로 직접 전이할 수 없다`() {
            assertFalse(RobotStatus.OFF_DUTY.canTransitionTo(RobotStatus.BUSY))
        }

        @Test
        @DisplayName("READY 상태에서 BUSY로 전이할 수 있다")
        fun `READY 상태에서 BUSY로 전이할 수 있다`() {
            assertTrue(RobotStatus.READY.canTransitionTo(RobotStatus.BUSY))
        }

        @Test
        @DisplayName("READY 상태에서 OFF_DUTY로 전이할 수 있다")
        fun `READY 상태에서 OFF_DUTY로 전이할 수 있다`() {
            assertTrue(RobotStatus.READY.canTransitionTo(RobotStatus.OFF_DUTY))
        }

        @Test
        @DisplayName("BUSY 상태에서 READY로 전이할 수 있다")
        fun `BUSY 상태에서 READY로 전이할 수 있다`() {
            assertTrue(RobotStatus.BUSY.canTransitionTo(RobotStatus.READY))
        }

        @Test
        @DisplayName("BUSY 상태에서 OFF_DUTY로 직접 전이할 수 없다")
        fun `BUSY 상태에서 OFF_DUTY로 직접 전이할 수 없다`() {
            assertFalse(RobotStatus.BUSY.canTransitionTo(RobotStatus.OFF_DUTY))
        }
    }

    @Nested
    @DisplayName("배달 가능 상태 테스트")
    inner class AvailabilityTest {
        @Test
        @DisplayName("READY 상태만 배달 가능하다")
        fun `READY 상태만 배달 가능하다`() {
            assertTrue(RobotStatus.READY.isAvailableForDelivery())
        }

        @Test
        @DisplayName("BUSY 상태는 배달 불가능하다")
        fun `BUSY 상태는 배달 불가능하다`() {
            assertFalse(RobotStatus.BUSY.isAvailableForDelivery())
        }

        @Test
        @DisplayName("OFF_DUTY 상태는 배달 불가능하다")
        fun `OFF_DUTY 상태는 배달 불가능하다`() {
            assertFalse(RobotStatus.OFF_DUTY.isAvailableForDelivery())
        }
    }
}
