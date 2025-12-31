package com.robotdelivery.domain.delivery.vo

import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.robot.vo.RouteResult

sealed class DeliveryCreationResult {
    data class Success(
        val delivery: Delivery,
        val routeResult: RouteResult,
    ) : DeliveryCreationResult()

    data object RouteNotAvailable : DeliveryCreationResult()
}

