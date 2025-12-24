package com.robotdelivery.application.command

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.Destination

data class CreateDeliveryCommand(
    val pickupDestination: DestinationInfo,
    val deliveryDestination: DestinationInfo,
    val phoneNumber: String,
) {
    fun toDelivery(): Delivery =
        Delivery(
            pickupDestination = pickupDestination.toDestination(),
            deliveryDestination = deliveryDestination.toDestination(),
            phoneNumber = phoneNumber,
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
