package com.robotdelivery.domain.order

import com.robotdelivery.domain.common.BaseEntity
import com.robotdelivery.domain.common.vo.OrderId
import com.robotdelivery.domain.common.vo.OrderNo
import com.robotdelivery.domain.common.vo.Volume
import com.robotdelivery.domain.delivery.vo.Destination
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    val id: Long = 0L,
    orderNo: OrderNo,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "address", column = Column(name = "pickup_address")),
        AttributeOverride(name = "addressDetail", column = Column(name = "pickup_address_detail")),
        AttributeOverride(name = "location.latitude", column = Column(name = "pickup_latitude")),
        AttributeOverride(name = "location.longitude", column = Column(name = "pickup_longitude")),
    )
    val pickupDestination: Destination,
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "address", column = Column(name = "delivery_address")),
        AttributeOverride(name = "addressDetail", column = Column(name = "delivery_address_detail")),
        AttributeOverride(name = "location.latitude", column = Column(name = "delivery_latitude")),
        AttributeOverride(name = "location.longitude", column = Column(name = "delivery_longitude")),
    )
    val deliveryDestination: Destination,
    @Column(nullable = false)
    val phoneNumber: String,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "order_items",
        joinColumns = [JoinColumn(name = "order_id")],
    )
    val items: List<OrderItem> = emptyList(),
) : BaseEntity<Order>() {
    @Column(name = "order_no", nullable = false, unique = true)
    private val _orderNo: String = orderNo.value

    val orderNo: OrderNo
        get() = OrderNo(_orderNo)

    init {
        require(phoneNumber.isNotBlank()) { "전화번호는 비어있을 수 없습니다." }
        require(items.isNotEmpty()) { "주문 항목은 최소 1개 이상이어야 합니다." }
    }

    fun calculateTotalVolume(): Volume =
        items
            .map { it.calculateTotalVolume() }
            .fold(Volume.ZERO) { acc, volume -> acc + volume }

    fun getOrderId(): OrderId {
        check(id > 0) { "아직 persist되지 않은 엔티티입니다." }
        return OrderId(id)
    }
}
