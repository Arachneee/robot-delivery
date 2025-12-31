package com.robotdelivery.view.admin.dto

import com.robotdelivery.domain.delivery.vo.DeliveryStatus

data class ChangeStatusResponse(
    val deliveryId: Long,
    val previousStatus: DeliveryStatus,
    val currentStatus: DeliveryStatus,
    val message: String,
) {
    companion object {
        fun of(
            deliveryId: Long,
            previousStatus: DeliveryStatus,
            currentStatus: DeliveryStatus,
        ): ChangeStatusResponse =
            ChangeStatusResponse(
                deliveryId = deliveryId,
                previousStatus = previousStatus,
                currentStatus = currentStatus,
                message = "상태가 ${previousStatus.name}에서 ${currentStatus.name}(으)로 변경되었습니다.",
            )
    }
}
