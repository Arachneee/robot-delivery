package com.robotdelivery.application.query.vo

import com.robotdelivery.domain.common.vo.Location

data class EstimateDeliveryTimeQuery(
    val pickupLocation: Location,
    val deliveryLocation: Location,
)

