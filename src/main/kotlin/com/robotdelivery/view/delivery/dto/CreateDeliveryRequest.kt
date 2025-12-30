package com.robotdelivery.view.delivery.dto

import com.robotdelivery.application.command.vo.CreateDeliveryCommand
import com.robotdelivery.application.command.vo.DestinationInfo
import com.robotdelivery.application.command.vo.OrderItemInfo
import java.math.BigDecimal

data class CreateDeliveryRequest(
    val orderNo: String,
    val pickupAddress: String,
    val pickupAddressDetail: String?,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val deliveryAddress: String,
    val deliveryAddressDetail: String?,
    val deliveryLatitude: Double,
    val deliveryLongitude: Double,
    val phoneNumber: String,
    val items: List<OrderItemRequest>,
) {
    fun toCommand(): CreateDeliveryCommand =
        CreateDeliveryCommand(
            orderNo = orderNo,
            pickupDestination =
                DestinationInfo(
                    address = pickupAddress,
                    addressDetail = pickupAddressDetail,
                    latitude = pickupLatitude,
                    longitude = pickupLongitude,
                ),
            deliveryDestination =
                DestinationInfo(
                    address = deliveryAddress,
                    addressDetail = deliveryAddressDetail,
                    latitude = deliveryLatitude,
                    longitude = deliveryLongitude,
                ),
            phoneNumber = phoneNumber,
            items = items.map { it.toOrderItemInfo() },
        )
}

data class OrderItemRequest(
    val name: String,
    val price: BigDecimal,
    val quantity: Int,
    val volume: Double,
) {
    fun toOrderItemInfo(): OrderItemInfo =
        OrderItemInfo(
            name = name,
            price = price,
            quantity = quantity,
            volume = volume,
        )
}
