package com.robotdelivery.domain.delivery

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DeliveryStatus 테스트")
class DeliveryStatusTest {
    @Nested
    @DisplayName("상태 전이 테스트")
    inner class TransitionTest {
        @Test
        @DisplayName("PENDING 상태에서 ASSIGNED로 전이할 수 있다")
        fun `PENDING 상태에서 ASSIGNED로 전이할 수 있다`() {
            assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.ASSIGNED)).isTrue()
        }

        @Test
        @DisplayName("PENDING 상태에서 CANCELED로 전이할 수 있다")
        fun `PENDING 상태에서 CANCELED로 전이할 수 있다`() {
            assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.CANCELED)).isTrue()
        }

        @Test
        @DisplayName("PENDING 상태에서 DELIVERING으로 직접 전이할 수 없다")
        fun `PENDING 상태에서 DELIVERING으로 직접 전이할 수 없다`() {
            assertThat(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.DELIVERING)).isFalse()
        }

        @Test
        @DisplayName("ASSIGNED 상태에서 PICKUP_ARRIVED로 전이할 수 있다")
        fun `ASSIGNED 상태에서 PICKUP_ARRIVED로 전이할 수 있다`() {
            assertThat(DeliveryStatus.ASSIGNED.canTransitionTo(DeliveryStatus.PICKUP_ARRIVED)).isTrue()
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태에서 PICKING_UP으로 전이할 수 있다")
        fun `PICKUP_ARRIVED 상태에서 PICKING_UP으로 전이할 수 있다`() {
            assertThat(DeliveryStatus.PICKUP_ARRIVED.canTransitionTo(DeliveryStatus.PICKING_UP)).isTrue()
        }

        @Test
        @DisplayName("PICKING_UP 상태에서 DELIVERING으로 전이할 수 있다")
        fun `PICKING_UP 상태에서 DELIVERING으로 전이할 수 있다`() {
            assertThat(DeliveryStatus.PICKING_UP.canTransitionTo(DeliveryStatus.DELIVERING)).isTrue()
        }

        @Test
        @DisplayName("DELIVERING 상태에서 DELIVERY_ARRIVED로 전이할 수 있다")
        fun `DELIVERING 상태에서 DELIVERY_ARRIVED로 전이할 수 있다`() {
            assertThat(DeliveryStatus.DELIVERING.canTransitionTo(DeliveryStatus.DELIVERY_ARRIVED)).isTrue()
        }

        @Test
        @DisplayName("DELIVERY_ARRIVED 상태에서 DROPPING_OFF로 전이할 수 있다")
        fun `DELIVERY_ARRIVED 상태에서 DROPPING_OFF로 전이할 수 있다`() {
            assertThat(DeliveryStatus.DELIVERY_ARRIVED.canTransitionTo(DeliveryStatus.DROPPING_OFF)).isTrue()
        }

        @Test
        @DisplayName("DROPPING_OFF 상태에서 COMPLETED로 전이할 수 있다")
        fun `DROPPING_OFF 상태에서 COMPLETED로 전이할 수 있다`() {
            assertThat(DeliveryStatus.DROPPING_OFF.canTransitionTo(DeliveryStatus.COMPLETED)).isTrue()
        }

        @Test
        @DisplayName("COMPLETED 상태에서는 다른 상태로 전이할 수 없다")
        fun `COMPLETED 상태에서는 다른 상태로 전이할 수 없다`() {
            DeliveryStatus.entries.forEach { status ->
                assertThat(DeliveryStatus.COMPLETED.canTransitionTo(status)).isFalse()
            }
        }

        @Test
        @DisplayName("CANCELED 상태에서는 다른 상태로 전이할 수 없다")
        fun `CANCELED 상태에서는 다른 상태로 전이할 수 없다`() {
            DeliveryStatus.entries.forEach { status ->
                assertThat(DeliveryStatus.CANCELED.canTransitionTo(status)).isFalse()
            }
        }
    }

    @Nested
    @DisplayName("반품 상태 전이 테스트")
    inner class ReturnTransitionTest {
        @Test
        @DisplayName("PICKING_UP 상태에서 RETURNING으로 전이할 수 있다")
        fun `PICKING_UP 상태에서 RETURNING으로 전이할 수 있다`() {
            assertThat(DeliveryStatus.PICKING_UP.canTransitionTo(DeliveryStatus.RETURNING)).isTrue()
        }

        @Test
        @DisplayName("DELIVERING 상태에서 RETURNING으로 전이할 수 있다")
        fun `DELIVERING 상태에서 RETURNING으로 전이할 수 있다`() {
            assertThat(DeliveryStatus.DELIVERING.canTransitionTo(DeliveryStatus.RETURNING)).isTrue()
        }

        @Test
        @DisplayName("RETURNING 상태에서 RETURN_ARRIVED로 전이할 수 있다")
        fun `RETURNING 상태에서 RETURN_ARRIVED로 전이할 수 있다`() {
            assertThat(DeliveryStatus.RETURNING.canTransitionTo(DeliveryStatus.RETURN_ARRIVED)).isTrue()
        }

        @Test
        @DisplayName("RETURN_ARRIVED 상태에서 RETURNING_OFF로 전이할 수 있다")
        fun `RETURN_ARRIVED 상태에서 RETURNING_OFF로 전이할 수 있다`() {
            assertThat(DeliveryStatus.RETURN_ARRIVED.canTransitionTo(DeliveryStatus.RETURNING_OFF)).isTrue()
        }

        @Test
        @DisplayName("RETURNING_OFF 상태에서 RETURN_COMPLETED로 전이할 수 있다")
        fun `RETURNING_OFF 상태에서 RETURN_COMPLETED로 전이할 수 있다`() {
            assertThat(DeliveryStatus.RETURNING_OFF.canTransitionTo(DeliveryStatus.RETURN_COMPLETED)).isTrue()
        }
    }

    @Nested
    @DisplayName("취소 가능 상태 테스트")
    inner class CancelableTest {
        @Test
        @DisplayName("PENDING 상태는 취소 가능하다")
        fun `PENDING 상태는 취소 가능하다`() {
            assertThat(DeliveryStatus.PENDING.isCancelable()).isTrue()
        }

        @Test
        @DisplayName("ASSIGNED 상태는 취소 가능하다")
        fun `ASSIGNED 상태는 취소 가능하다`() {
            assertThat(DeliveryStatus.ASSIGNED.isCancelable()).isTrue()
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태는 취소 가능하다")
        fun `PICKUP_ARRIVED 상태는 취소 가능하다`() {
            assertThat(DeliveryStatus.PICKUP_ARRIVED.isCancelable()).isTrue()
        }

        @Test
        @DisplayName("DELIVERING 상태는 직접 취소 불가능하다")
        fun `DELIVERING 상태는 직접 취소 불가능하다`() {
            assertThat(DeliveryStatus.DELIVERING.isCancelable()).isFalse()
        }

        @Test
        @DisplayName("COMPLETED 상태는 취소 불가능하다")
        fun `COMPLETED 상태는 취소 불가능하다`() {
            assertThat(DeliveryStatus.COMPLETED.isCancelable()).isFalse()
        }
    }

    @Nested
    @DisplayName("반품 필요 상태 테스트")
    inner class RequiresReturnTest {
        @Test
        @DisplayName("PICKING_UP 상태는 반품이 필요하다")
        fun `PICKING_UP 상태는 반품이 필요하다`() {
            assertThat(DeliveryStatus.PICKING_UP.requiresReturn()).isTrue()
        }

        @Test
        @DisplayName("DELIVERING 상태는 반품이 필요하다")
        fun `DELIVERING 상태는 반품이 필요하다`() {
            assertThat(DeliveryStatus.DELIVERING.requiresReturn()).isTrue()
        }

        @Test
        @DisplayName("DELIVERY_ARRIVED 상태는 반품이 필요하다")
        fun `DELIVERY_ARRIVED 상태는 반품이 필요하다`() {
            assertThat(DeliveryStatus.DELIVERY_ARRIVED.requiresReturn()).isTrue()
        }

        @Test
        @DisplayName("DROPPING_OFF 상태는 반품이 필요하다")
        fun `DROPPING_OFF 상태는 반품이 필요하다`() {
            assertThat(DeliveryStatus.DROPPING_OFF.requiresReturn()).isTrue()
        }

        @Test
        @DisplayName("PENDING 상태는 반품이 필요하지 않다")
        fun `PENDING 상태는 반품이 필요하지 않다`() {
            assertThat(DeliveryStatus.PENDING.requiresReturn()).isFalse()
        }

        @Test
        @DisplayName("ASSIGNED 상태는 반품이 필요하지 않다")
        fun `ASSIGNED 상태는 반품이 필요하지 않다`() {
            assertThat(DeliveryStatus.ASSIGNED.requiresReturn()).isFalse()
        }
    }

    @Nested
    @DisplayName("배차 취소 가능 상태 테스트")
    inner class UnassignableTest {
        @Test
        @DisplayName("ASSIGNED 상태는 배차 취소 가능하다")
        fun `ASSIGNED 상태는 배차 취소 가능하다`() {
            assertThat(DeliveryStatus.ASSIGNED.isUnassignable()).isTrue()
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태는 배차 취소 가능하다")
        fun `PICKUP_ARRIVED 상태는 배차 취소 가능하다`() {
            assertThat(DeliveryStatus.PICKUP_ARRIVED.isUnassignable()).isTrue()
        }

        @Test
        @DisplayName("PICKING_UP 상태는 배차 취소 가능하다")
        fun `PICKING_UP 상태는 배차 취소 가능하다`() {
            assertThat(DeliveryStatus.PICKING_UP.isUnassignable()).isTrue()
        }

        @Test
        @DisplayName("PENDING 상태는 배차 취소 불가능하다")
        fun `PENDING 상태는 배차 취소 불가능하다`() {
            assertThat(DeliveryStatus.PENDING.isUnassignable()).isFalse()
        }

        @Test
        @DisplayName("DELIVERING 상태는 배차 취소 불가능하다")
        fun `DELIVERING 상태는 배차 취소 불가능하다`() {
            assertThat(DeliveryStatus.DELIVERING.isUnassignable()).isFalse()
        }

        @Test
        @DisplayName("COMPLETED 상태는 배차 취소 불가능하다")
        fun `COMPLETED 상태는 배차 취소 불가능하다`() {
            assertThat(DeliveryStatus.COMPLETED.isUnassignable()).isFalse()
        }
    }

    @Nested
    @DisplayName("배차 변경 가능 상태 테스트")
    inner class ReassignableTest {
        @Test
        @DisplayName("ASSIGNED 상태는 배차 변경 가능하다")
        fun `ASSIGNED 상태는 배차 변경 가능하다`() {
            assertThat(DeliveryStatus.ASSIGNED.isReassignable()).isTrue()
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태는 배차 변경 가능하다")
        fun `PICKUP_ARRIVED 상태는 배차 변경 가능하다`() {
            assertThat(DeliveryStatus.PICKUP_ARRIVED.isReassignable()).isTrue()
        }

        @Test
        @DisplayName("PICKING_UP 상태는 배차 변경 가능하다")
        fun `PICKING_UP 상태는 배차 변경 가능하다`() {
            assertThat(DeliveryStatus.PICKING_UP.isReassignable()).isTrue()
        }

        @Test
        @DisplayName("DELIVERING 상태는 배차 변경 가능하다")
        fun `DELIVERING 상태는 배차 변경 가능하다`() {
            assertThat(DeliveryStatus.DELIVERING.isReassignable()).isTrue()
        }

        @Test
        @DisplayName("DELIVERY_ARRIVED 상태는 배차 변경 가능하다")
        fun `DELIVERY_ARRIVED 상태는 배차 변경 가능하다`() {
            assertThat(DeliveryStatus.DELIVERY_ARRIVED.isReassignable()).isTrue()
        }

        @Test
        @DisplayName("DROPPING_OFF 상태는 배차 변경 가능하다")
        fun `DROPPING_OFF 상태는 배차 변경 가능하다`() {
            assertThat(DeliveryStatus.DROPPING_OFF.isReassignable()).isTrue()
        }

        @Test
        @DisplayName("PENDING 상태는 배차 변경 불가능하다")
        fun `PENDING 상태는 배차 변경 불가능하다`() {
            assertThat(DeliveryStatus.PENDING.isReassignable()).isFalse()
        }

        @Test
        @DisplayName("COMPLETED 상태는 배차 변경 불가능하다")
        fun `COMPLETED 상태는 배차 변경 불가능하다`() {
            assertThat(DeliveryStatus.COMPLETED.isReassignable()).isFalse()
        }

        @Test
        @DisplayName("CANCELED 상태는 배차 변경 불가능하다")
        fun `CANCELED 상태는 배차 변경 불가능하다`() {
            assertThat(DeliveryStatus.CANCELED.isReassignable()).isFalse()
        }

        @Test
        @DisplayName("RETURNING 상태는 배차 변경 불가능하다")
        fun `RETURNING 상태는 배차 변경 불가능하다`() {
            assertThat(DeliveryStatus.RETURNING.isReassignable()).isFalse()
        }
    }

    @Nested
    @DisplayName("종료 상태 테스트")
    inner class TerminalTest {
        @Test
        @DisplayName("COMPLETED 상태는 종료 상태다")
        fun `COMPLETED 상태는 종료 상태다`() {
            assertThat(DeliveryStatus.COMPLETED.isTerminal()).isTrue()
        }

        @Test
        @DisplayName("CANCELED 상태는 종료 상태다")
        fun `CANCELED 상태는 종료 상태다`() {
            assertThat(DeliveryStatus.CANCELED.isTerminal()).isTrue()
        }

        @Test
        @DisplayName("RETURN_COMPLETED 상태는 종료 상태다")
        fun `RETURN_COMPLETED 상태는 종료 상태다`() {
            assertThat(DeliveryStatus.RETURN_COMPLETED.isTerminal()).isTrue()
        }

        @Test
        @DisplayName("PENDING 상태는 종료 상태가 아니다")
        fun `PENDING 상태는 종료 상태가 아니다`() {
            assertThat(DeliveryStatus.PENDING.isTerminal()).isFalse()
        }

        @Test
        @DisplayName("ASSIGNED 상태는 종료 상태가 아니다")
        fun `ASSIGNED 상태는 종료 상태가 아니다`() {
            assertThat(DeliveryStatus.ASSIGNED.isTerminal()).isFalse()
        }

        @Test
        @DisplayName("DELIVERING 상태는 종료 상태가 아니다")
        fun `DELIVERING 상태는 종료 상태가 아니다`() {
            assertThat(DeliveryStatus.DELIVERING.isTerminal()).isFalse()
        }

        @Test
        @DisplayName("RETURNING 상태는 종료 상태가 아니다")
        fun `RETURNING 상태는 종료 상태가 아니다`() {
            assertThat(DeliveryStatus.RETURNING.isTerminal()).isFalse()
        }
    }
}
