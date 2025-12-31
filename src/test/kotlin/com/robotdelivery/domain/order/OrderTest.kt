package com.robotdelivery.domain.order

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.OrderNo
import com.robotdelivery.domain.common.vo.Volume
import com.robotdelivery.domain.delivery.vo.Destination
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("Order 테스트")
class OrderTest {
    private lateinit var pickupDestination: Destination
    private lateinit var deliveryDestination: Destination

    @BeforeEach
    fun setUp() {
        pickupDestination =
            Destination(
                address = "서울시 중구 세종대로 110",
                addressDetail = "시청역 1번 출구",
                location = Location(latitude = 37.5665, longitude = 126.9780),
            )
        deliveryDestination =
            Destination(
                address = "서울시 강남구 테헤란로 1",
                addressDetail = "강남역 2번 출구",
                location = Location(latitude = 37.4979, longitude = 127.0276),
            )
    }

    private fun createOrder(
        id: Long = 1L,
        orderNo: OrderNo = OrderNo("ORDER-001"),
        items: List<OrderItem> =
            listOf(
                OrderItem(
                    name = "테스트 상품",
                    price = BigDecimal("10000"),
                    quantity = 1,
                    volume = 5.0,
                ),
            ),
    ): Order =
        Order(
            id = id,
            orderNo = orderNo,
            pickupDestination = pickupDestination,
            deliveryDestination = deliveryDestination,
            phoneNumber = "010-1234-5678",
            items = items,
        )

    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("유효한 값으로 Order를 생성할 수 있다")
        fun `유효한 값으로 Order를 생성할 수 있다`() {
            val order = createOrder()

            assertThat(order.orderNo).isEqualTo(OrderNo("ORDER-001"))
            assertThat(order.pickupDestination).isEqualTo(pickupDestination)
            assertThat(order.deliveryDestination).isEqualTo(deliveryDestination)
            assertThat(order.phoneNumber).isEqualTo("010-1234-5678")
            assertThat(order.items).hasSize(1)
        }

        @Test
        @DisplayName("빈 전화번호로 Order를 생성하면 예외가 발생한다")
        fun `빈 전화번호로 Order를 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                Order(
                    id = 1L,
                    orderNo = OrderNo("ORDER-001"),
                    pickupDestination = pickupDestination,
                    deliveryDestination = deliveryDestination,
                    phoneNumber = "",
                    items =
                        listOf(
                            OrderItem(
                                name = "테스트 상품",
                                price = BigDecimal("10000"),
                                quantity = 1,
                                volume = 5.0,
                            ),
                        ),
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("전화번호는 비어있을 수 없습니다")
        }

        @Test
        @DisplayName("빈 주문 항목으로 Order를 생성하면 예외가 발생한다")
        fun `빈 주문 항목으로 Order를 생성하면 예외가 발생한다`() {
            assertThatThrownBy {
                Order(
                    id = 1L,
                    orderNo = OrderNo("ORDER-001"),
                    pickupDestination = pickupDestination,
                    deliveryDestination = deliveryDestination,
                    phoneNumber = "010-1234-5678",
                    items = emptyList(),
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("주문 항목은 최소 1개 이상이어야 합니다")
        }
    }

    @Nested
    @DisplayName("총 부피 계산 테스트")
    inner class CalculateTotalVolumeTest {
        @Test
        @DisplayName("단일 상품의 총 부피를 계산한다")
        fun `단일 상품의 총 부피를 계산한다`() {
            val order =
                createOrder(
                    items =
                        listOf(
                            OrderItem(
                                name = "상품 A",
                                price = BigDecimal("10000"),
                                quantity = 2,
                                volume = 5.0,
                            ),
                        ),
                )

            val totalVolume = order.calculateTotalVolume()

            assertThat(totalVolume).isEqualTo(Volume(10.0))
        }

        @Test
        @DisplayName("여러 상품의 총 부피를 계산한다")
        fun `여러 상품의 총 부피를 계산한다`() {
            val order =
                createOrder(
                    items =
                        listOf(
                            OrderItem(
                                name = "상품 A",
                                price = BigDecimal("10000"),
                                quantity = 2,
                                volume = 5.0,
                            ),
                            OrderItem(
                                name = "상품 B",
                                price = BigDecimal("20000"),
                                quantity = 3,
                                volume = 3.0,
                            ),
                        ),
                )

            val totalVolume = order.calculateTotalVolume()

            assertThat(totalVolume).isEqualTo(Volume(19.0))
        }
    }

    @Nested
    @DisplayName("OrderId 조회 테스트")
    inner class GetOrderIdTest {
        @Test
        @DisplayName("persist된 Order의 OrderId를 조회할 수 있다")
        fun `persist된 Order의 OrderId를 조회할 수 있다`() {
            val order = createOrder(id = 5L)

            val orderId = order.getOrderId()

            assertThat(orderId.value).isEqualTo(5L)
        }

        @Test
        @DisplayName("persist되지 않은 Order의 OrderId를 조회하면 예외가 발생한다")
        fun `persist되지 않은 Order의 OrderId를 조회하면 예외가 발생한다`() {
            val order = createOrder(id = 0L)

            assertThatThrownBy { order.getOrderId() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("아직 persist되지 않은 엔티티입니다")
        }
    }
}
