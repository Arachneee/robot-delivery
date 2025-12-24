package com.robotdelivery.domain.delivery

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
            assertTrue(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.ASSIGNED))
        }

        @Test
        @DisplayName("PENDING 상태에서 CANCELED로 전이할 수 있다")
        fun `PENDING 상태에서 CANCELED로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.CANCELED))
        }

        @Test
        @DisplayName("PENDING 상태에서 DELIVERING으로 직접 전이할 수 없다")
        fun `PENDING 상태에서 DELIVERING으로 직접 전이할 수 없다`() {
            assertFalse(DeliveryStatus.PENDING.canTransitionTo(DeliveryStatus.DELIVERING))
        }

        @Test
        @DisplayName("ASSIGNED 상태에서 PICKUP_ARRIVED로 전이할 수 있다")
        fun `ASSIGNED 상태에서 PICKUP_ARRIVED로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.ASSIGNED.canTransitionTo(DeliveryStatus.PICKUP_ARRIVED))
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태에서 PICKING_UP으로 전이할 수 있다")
        fun `PICKUP_ARRIVED 상태에서 PICKING_UP으로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.PICKUP_ARRIVED.canTransitionTo(DeliveryStatus.PICKING_UP))
        }

        @Test
        @DisplayName("PICKING_UP 상태에서 DELIVERING으로 전이할 수 있다")
        fun `PICKING_UP 상태에서 DELIVERING으로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.PICKING_UP.canTransitionTo(DeliveryStatus.DELIVERING))
        }

        @Test
        @DisplayName("DELIVERING 상태에서 DELIVERY_ARRIVED로 전이할 수 있다")
        fun `DELIVERING 상태에서 DELIVERY_ARRIVED로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.DELIVERING.canTransitionTo(DeliveryStatus.DELIVERY_ARRIVED))
        }

        @Test
        @DisplayName("DELIVERY_ARRIVED 상태에서 DROPPING_OFF로 전이할 수 있다")
        fun `DELIVERY_ARRIVED 상태에서 DROPPING_OFF로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.DELIVERY_ARRIVED.canTransitionTo(DeliveryStatus.DROPPING_OFF))
        }

        @Test
        @DisplayName("DROPPING_OFF 상태에서 COMPLETED로 전이할 수 있다")
        fun `DROPPING_OFF 상태에서 COMPLETED로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.DROPPING_OFF.canTransitionTo(DeliveryStatus.COMPLETED))
        }

        @Test
        @DisplayName("COMPLETED 상태에서는 다른 상태로 전이할 수 없다")
        fun `COMPLETED 상태에서는 다른 상태로 전이할 수 없다`() {
            DeliveryStatus.entries.forEach { status ->
                assertFalse(DeliveryStatus.COMPLETED.canTransitionTo(status))
            }
        }

        @Test
        @DisplayName("CANCELED 상태에서는 다른 상태로 전이할 수 없다")
        fun `CANCELED 상태에서는 다른 상태로 전이할 수 없다`() {
            DeliveryStatus.entries.forEach { status ->
                assertFalse(DeliveryStatus.CANCELED.canTransitionTo(status))
            }
        }
    }

    @Nested
    @DisplayName("반품 상태 전이 테스트")
    inner class ReturnTransitionTest {
        @Test
        @DisplayName("PICKING_UP 상태에서 RETURNING으로 전이할 수 있다")
        fun `PICKING_UP 상태에서 RETURNING으로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.PICKING_UP.canTransitionTo(DeliveryStatus.RETURNING))
        }

        @Test
        @DisplayName("DELIVERING 상태에서 RETURNING으로 전이할 수 있다")
        fun `DELIVERING 상태에서 RETURNING으로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.DELIVERING.canTransitionTo(DeliveryStatus.RETURNING))
        }

        @Test
        @DisplayName("RETURNING 상태에서 RETURN_ARRIVED로 전이할 수 있다")
        fun `RETURNING 상태에서 RETURN_ARRIVED로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.RETURNING.canTransitionTo(DeliveryStatus.RETURN_ARRIVED))
        }

        @Test
        @DisplayName("RETURN_ARRIVED 상태에서 RETURNING_OFF로 전이할 수 있다")
        fun `RETURN_ARRIVED 상태에서 RETURNING_OFF로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.RETURN_ARRIVED.canTransitionTo(DeliveryStatus.RETURNING_OFF))
        }

        @Test
        @DisplayName("RETURNING_OFF 상태에서 RETURN_COMPLETED로 전이할 수 있다")
        fun `RETURNING_OFF 상태에서 RETURN_COMPLETED로 전이할 수 있다`() {
            assertTrue(DeliveryStatus.RETURNING_OFF.canTransitionTo(DeliveryStatus.RETURN_COMPLETED))
        }
    }

    @Nested
    @DisplayName("취소 가능 상태 테스트")
    inner class CancelableTest {
        @Test
        @DisplayName("PENDING 상태는 취소 가능하다")
        fun `PENDING 상태는 취소 가능하다`() {
            assertTrue(DeliveryStatus.PENDING.isCancelable())
        }

        @Test
        @DisplayName("ASSIGNED 상태는 취소 가능하다")
        fun `ASSIGNED 상태는 취소 가능하다`() {
            assertTrue(DeliveryStatus.ASSIGNED.isCancelable())
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태는 취소 가능하다")
        fun `PICKUP_ARRIVED 상태는 취소 가능하다`() {
            assertTrue(DeliveryStatus.PICKUP_ARRIVED.isCancelable())
        }

        @Test
        @DisplayName("DELIVERING 상태는 직접 취소 불가능하다")
        fun `DELIVERING 상태는 직접 취소 불가능하다`() {
            assertFalse(DeliveryStatus.DELIVERING.isCancelable())
        }

        @Test
        @DisplayName("COMPLETED 상태는 취소 불가능하다")
        fun `COMPLETED 상태는 취소 불가능하다`() {
            assertFalse(DeliveryStatus.COMPLETED.isCancelable())
        }
    }

    @Nested
    @DisplayName("반품 필요 상태 테스트")
    inner class RequiresReturnTest {
        @Test
        @DisplayName("PICKING_UP 상태는 반품이 필요하다")
        fun `PICKING_UP 상태는 반품이 필요하다`() {
            assertTrue(DeliveryStatus.PICKING_UP.requiresReturn())
        }

        @Test
        @DisplayName("DELIVERING 상태는 반품이 필요하다")
        fun `DELIVERING 상태는 반품이 필요하다`() {
            assertTrue(DeliveryStatus.DELIVERING.requiresReturn())
        }

        @Test
        @DisplayName("DELIVERY_ARRIVED 상태는 반품이 필요하다")
        fun `DELIVERY_ARRIVED 상태는 반품이 필요하다`() {
            assertTrue(DeliveryStatus.DELIVERY_ARRIVED.requiresReturn())
        }

        @Test
        @DisplayName("DROPPING_OFF 상태는 반품이 필요하다")
        fun `DROPPING_OFF 상태는 반품이 필요하다`() {
            assertTrue(DeliveryStatus.DROPPING_OFF.requiresReturn())
        }

        @Test
        @DisplayName("PENDING 상태는 반품이 필요하지 않다")
        fun `PENDING 상태는 반품이 필요하지 않다`() {
            assertFalse(DeliveryStatus.PENDING.requiresReturn())
        }

        @Test
        @DisplayName("ASSIGNED 상태는 반품이 필요하지 않다")
        fun `ASSIGNED 상태는 반품이 필요하지 않다`() {
            assertFalse(DeliveryStatus.ASSIGNED.requiresReturn())
        }
    }
}
