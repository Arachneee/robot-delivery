package com.robotdelivery.view.delivery.dto

import com.robotdelivery.application.query.vo.EstimateDeliveryTimeQuery
import com.robotdelivery.domain.common.vo.Location

data class EstimateDeliveryTimeRequest(
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val deliveryLatitude: Double,
    val deliveryLongitude: Double,
) {
    fun toQuery(): EstimateDeliveryTimeQuery =
        EstimateDeliveryTimeQuery(
            pickupLocation = Location(
                latitude = pickupLatitude,
                longitude = pickupLongitude,
            ),
            deliveryLocation = Location(
                latitude = deliveryLatitude,
                longitude = deliveryLongitude,
            ),
        )
}

