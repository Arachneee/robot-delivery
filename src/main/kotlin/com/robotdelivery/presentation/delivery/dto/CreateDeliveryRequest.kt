package com.robotdelivery.presentation.delivery.dto

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
)
