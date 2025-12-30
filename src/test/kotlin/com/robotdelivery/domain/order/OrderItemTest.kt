package com.robotdelivery.domain.order

import com.robotdelivery.domain.common.Volume
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("OrderItem 테스트")
class OrderItemTest {
    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("유효한 값으로 OrderItem을 생성할 수 있다")
        fun `유효한 값으로 OrderItem을 생성할 수 있다`() {
            val orderItem = OrderItem(
                name = "테스트 상품",
                price = BigDecimal("10000"),
                quantity = 2,
                volume = 5.0,
            )

            assertThat(orderItem.name).isEqualTo("테스트 상품")
            assertThat(orderItem.price).isEqualTo(BigDecimal("10000"))
            assertThat(orderItem.quantity).isEqualTo(2)
            assertThat(orderItem.volume).isEqualTo(5.0)
        }

        @Test
        @DisplayName("빈 이름으로 OrderItem을 생성하면 예외가 발생한다")
        fun `빈 이름으로 OrderItem을 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                OrderItem(
                    name = "",
                    price = BigDecimal("10000"),
                    quantity = 1,
                    volume = 1.0,
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("물품 이름은 비어있을 수 없습니다")
        }

        @Test
        @DisplayName("음수 가격으로 OrderItem을 생성하면 예외가 발생한다")
        fun `음수 가격으로 OrderItem을 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                OrderItem(
                    name = "테스트 상품",
                    price = BigDecimal("-1000"),
                    quantity = 1,
                    volume = 1.0,
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("가격은 0 이상이어야 합니다")
        }

        @Test
        @DisplayName("0 수량으로 OrderItem을 생성하면 예외가 발생한다")
        fun `0 수량으로 OrderItem을 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                OrderItem(
                    name = "테스트 상품",
                    price = BigDecimal("10000"),
                    quantity = 0,
                    volume = 1.0,
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("수량은 1 이상이어야 합니다")
        }

        @Test
        @DisplayName("음수 부피로 OrderItem을 생성하면 예외가 발생한다")
        fun `음수 부피로 OrderItem을 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                OrderItem(
                    name = "테스트 상품",
                    price = BigDecimal("10000"),
                    quantity = 1,
                    volume = -1.0,
                )
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("부피는 0 이상이어야 합니다")
        }
    }

    @Nested
    @DisplayName("총 부피 계산 테스트")
    inner class CalculateTotalVolumeTest {
        @Test
        @DisplayName("수량이 1일 때 부피를 반환한다")
        fun `수량이 1일 때 부피를 반환한다`() {
            val orderItem = OrderItem(
                name = "테스트 상품",
                price = BigDecimal("10000"),
                quantity = 1,
                volume = 5.0,
            )

            val totalVolume = orderItem.calculateTotalVolume()

            assertThat(totalVolume).isEqualTo(Volume(5.0))
        }

        @Test
        @DisplayName("수량이 여러 개일 때 부피 x 수량을 반환한다")
        fun `수량이 여러 개일 때 부피 x 수량을 반환한다`() {
            val orderItem = OrderItem(
                name = "테스트 상품",
                price = BigDecimal("10000"),
                quantity = 3,
                volume = 5.0,
            )

            val totalVolume = orderItem.calculateTotalVolume()

            assertThat(totalVolume).isEqualTo(Volume(15.0))
        }
    }
}

