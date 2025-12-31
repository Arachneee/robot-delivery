package com.robotdelivery.application.command.vo

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.OrderId
import com.robotdelivery.domain.common.vo.OrderNo
import com.robotdelivery.domain.common.vo.Volume
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.vo.Destination
import com.robotdelivery.domain.order.Order
import com.robotdelivery.domain.order.OrderItem
import java.math.BigDecimal

data class CreateDeliveryCommand(
    val orderNo: String,
    val pickupDestination: DestinationInfo,
    val deliveryDestination: DestinationInfo,
    val phoneNumber: String,
    val items: List<OrderItemInfo>,
) {
    fun toOrder(): Order =
        Order(
            orderNo = OrderNo(orderNo),
            pickupDestination = pickupDestination.toDestination(),
            deliveryDestination = deliveryDestination.toDestination(),
            phoneNumber = phoneNumber,
            items = items.map { it.toOrderItem() },
        )

    fun toDelivery(
        orderId: OrderId,
        totalVolume: Volume,
    ): Delivery =
        Delivery(
            orderId = orderId,
            pickupDestination = pickupDestination.toDestination(),
            deliveryDestination = deliveryDestination.toDestination(),
            phoneNumber = phoneNumber,
            totalVolume = totalVolume,
        )
}

data class DestinationInfo(
    val address: String,
    val addressDetail: String?,
    val latitude: Double,
    val longitude: Double,
) {
    fun toDestination(): Destination =
        Destination(
            address = address,
            addressDetail = addressDetail,
            location = Location(latitude, longitude),
        )
}

data class OrderItemInfo(
    val name: String,
    val price: BigDecimal,
    val quantity: Int,
    val volume: Double,
) {
    fun toOrderItem(): OrderItem =
        OrderItem(
            name = name,
            price = price,
            quantity = quantity,
            volume = volume,
        )
}
