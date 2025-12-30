package com.robotdelivery.view.delivery.dto

sealed class DeliveryResponse(
    open val deliveryId: Long,
    open val message: String,
)

data class CreateDeliveryResponse(
    override val deliveryId: Long,
    override val message: String = "배달이 성공적으로 생성되었습니다.",
) : DeliveryResponse(deliveryId, message)

data class CreateAdditionalDeliveryResponse(
    override val deliveryId: Long,
    val orderNo: String,
    override val message: String = "추가 배달이 성공적으로 생성되었습니다.",
) : DeliveryResponse(deliveryId, message)

data class StartDeliveryResponse(
    override val deliveryId: Long,
    override val message: String = "배송이 시작되었습니다.",
) : DeliveryResponse(deliveryId, message)

data class CompleteDeliveryResponse(
    override val deliveryId: Long,
    override val message: String = "배달이 완료되었습니다.",
) : DeliveryResponse(deliveryId, message)

data class CompleteReturnResponse(
    override val deliveryId: Long,
    override val message: String = "회수가 완료되었습니다.",
) : DeliveryResponse(deliveryId, message)

data class OpenDoorResponse(
    override val deliveryId: Long,
    override val message: String = "로봇 문이 열렸습니다.",
) : DeliveryResponse(deliveryId, message)

data class UnassignRobotResponse(
    override val deliveryId: Long,
    override val message: String = "배차가 취소되었습니다.",
) : DeliveryResponse(deliveryId, message)

data class CancelDeliveryResponse(
    override val deliveryId: Long,
    val requiresReturn: Boolean,
    override val message: String,
) : DeliveryResponse(deliveryId, message) {
    companion object {
        fun of(deliveryId: Long, requiresReturn: Boolean): CancelDeliveryResponse =
            CancelDeliveryResponse(
                deliveryId = deliveryId,
                requiresReturn = requiresReturn,
                message = if (requiresReturn) {
                    "배달이 취소되었습니다. 물품이 픽업 위치로 회수됩니다."
                } else {
                    "배달이 취소되었습니다."
                },
            )
    }
}

data class ReassignRobotResponse(
    override val deliveryId: Long,
    val previousRobotId: Long?,
    val newRobotId: Long,
    override val message: String,
) : DeliveryResponse(deliveryId, message) {
    companion object {
        fun of(
            deliveryId: Long,
            previousRobotId: Long?,
            newRobotId: Long,
        ): ReassignRobotResponse =
            ReassignRobotResponse(
                deliveryId = deliveryId,
                previousRobotId = previousRobotId,
                newRobotId = newRobotId,
                message = if (previousRobotId == null) {
                    "로봇이 배차되었습니다."
                } else {
                    "배차가 변경되었습니다."
                },
            )
    }
}

