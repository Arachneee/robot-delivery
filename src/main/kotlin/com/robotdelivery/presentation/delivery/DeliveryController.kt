package com.robotdelivery.presentation.delivery

import com.robotdelivery.application.DeliveryService
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.presentation.delivery.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/api/deliveries")
class DeliveryController(
    private val deliveryService: DeliveryService,
) {
    @PostMapping
    fun createDelivery(
        @RequestBody request: CreateDeliveryRequest,
    ): ResponseEntity<CreateDeliveryResponse> {
        val deliveryId =
            deliveryService.createDelivery(
                pickupAddress = request.pickupAddress,
                pickupAddressDetail = request.pickupAddressDetail,
                pickupLatitude = request.pickupLatitude,
                pickupLongitude = request.pickupLongitude,
                deliveryAddress = request.deliveryAddress,
                deliveryAddressDetail = request.deliveryAddressDetail,
                deliveryLatitude = request.deliveryLatitude,
                deliveryLongitude = request.deliveryLongitude,
                phoneNumber = request.phoneNumber,
            )

        val response =
            CreateDeliveryResponse(
                deliveryId = deliveryId.value,
                message = "배달이 성공적으로 생성되었습니다.",
            )

        return ResponseEntity
            .created(URI.create("/api/deliveries/${deliveryId.value}"))
            .body(response)
    }

    @PostMapping("/{deliveryId}/start")
    fun startDelivery(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<StartDeliveryResponse> {
        deliveryService.startDelivery(DeliveryId(deliveryId))

        val response =
            StartDeliveryResponse(
                deliveryId = deliveryId,
                message = "배송이 시작되었습니다.",
            )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{deliveryId}/complete")
    fun completeDelivery(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<CompleteDeliveryResponse> {
        deliveryService.completeDelivery(DeliveryId(deliveryId))

        val response =
            CompleteDeliveryResponse(
                deliveryId = deliveryId,
                message = "배달이 완료되었습니다.",
            )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{deliveryId}/complete-return")
    fun completeReturn(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<CompleteReturnResponse> {
        deliveryService.completeReturn(DeliveryId(deliveryId))

        val response =
            CompleteReturnResponse(
                deliveryId = deliveryId,
                message = "회수가 완료되었습니다.",
            )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{deliveryId}/open-door")
    fun openDoor(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<OpenDoorResponse> {
        deliveryService.openDoor(DeliveryId(deliveryId))

        val response =
            OpenDoorResponse(
                deliveryId = deliveryId,
                message = "로봇 문이 열렸습니다.",
            )

        return ResponseEntity.ok(response)
    }
}
