package com.robotdelivery.view.admin

import com.robotdelivery.application.DeliveryService
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.view.admin.dto.ChangeStatusResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/deliveries")
class AdminDeliveryController(
    private val deliveryService: DeliveryService,
) {
    @PostMapping("/{deliveryId}/change-status")
    fun changeStatus(
        @PathVariable deliveryId: Long,
        @RequestParam targetStatus: DeliveryStatus,
    ): ResponseEntity<ChangeStatusResponse> {
        val result =
            deliveryService.changeStatus(
                deliveryId = DeliveryId(deliveryId),
                targetStatus = targetStatus,
            )

        return ResponseEntity.ok(
            ChangeStatusResponse.of(
                deliveryId = deliveryId,
                previousStatus = result.previousStatus,
                currentStatus = result.currentStatus,
            ),
        )
    }
}
