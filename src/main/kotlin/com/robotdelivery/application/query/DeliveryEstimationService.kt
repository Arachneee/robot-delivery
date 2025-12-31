package com.robotdelivery.application.query

import com.robotdelivery.application.query.vo.DeliveryTimeEstimation
import com.robotdelivery.application.query.vo.EstimateDeliveryTimeQuery
import com.robotdelivery.domain.robot.RobotMapClient
import org.springframework.stereotype.Service

@Service
class DeliveryEstimationService(
    private val robotMapClient: RobotMapClient,
) {
    fun estimateDeliveryTime(query: EstimateDeliveryTimeQuery): DeliveryTimeEstimation {
        val waypoints = listOf(query.pickupLocation, query.deliveryLocation)
        val routeResult = robotMapClient.findRoute(waypoints)

        check(routeResult.isAvailable) {
            "배달 경로를 찾을 수 없습니다."
        }

        return DeliveryTimeEstimation(
            estimatedPickupDuration = routeResult.toPickupDuration,
            estimatedDeliveryDuration = routeResult.toDeliveryDuration,
        )
    }
}
