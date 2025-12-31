package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.vo.OrderId
import com.robotdelivery.domain.common.vo.Volume
import com.robotdelivery.domain.delivery.vo.DeliveryCreationResult
import com.robotdelivery.domain.delivery.vo.Destination
import com.robotdelivery.domain.robot.RobotMapClient

class DeliveryFactory(
    private val robotMapClient: RobotMapClient,
) {
    fun create(
        pickupDestination: Destination,
        deliveryDestination: Destination,
        phoneNumber: String,
        orderId: OrderId? = null,
        totalVolume: Volume? = null,
    ): DeliveryCreationResult {
        val routeResult =
            robotMapClient.findRoute(
                listOf(pickupDestination.location, deliveryDestination.location),
            )

        if (!routeResult.isAvailable) {
            return DeliveryCreationResult.RouteNotAvailable
        }

        val delivery =
            Delivery(
                pickupDestination = pickupDestination,
                deliveryDestination = deliveryDestination,
                phoneNumber = phoneNumber,
                orderId = orderId,
                totalVolume = totalVolume,
            )

        return DeliveryCreationResult.Success(delivery, routeResult)
    }
}

