package com.robotdelivery.view.delivery.dto

import com.robotdelivery.application.command.vo.CreateDeliveryCommand
import com.robotdelivery.application.command.vo.DestinationInfo

data class CreateDeliveryRequest(
    val pickupAddress: String,
    val pickupAddressDetail: String?,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val deliveryAddress: String,
    val deliveryAddressDetail: String?,
    val deliveryLatitude: Double,
    val deliveryLongitude: Double,
    val phoneNumber: String,
) {
    fun toCommand(): CreateDeliveryCommand =
        CreateDeliveryCommand(
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
        )
}
