package com.robotdelivery.view.delivery

import com.robotdelivery.application.DeliveryService
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.view.delivery.dto.*
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
        val deliveryId = deliveryService.createDelivery(request.toCommand())

        val response = CreateDeliveryResponse(deliveryId = deliveryId.value)

        return ResponseEntity
            .created(URI.create("/api/deliveries/${deliveryId.value}"))
            .body(response)
    }

    @PostMapping("/{deliveryId}/start")
    fun startDelivery(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<StartDeliveryResponse> {
        deliveryService.startDelivery(DeliveryId(deliveryId))
        return ResponseEntity.ok(StartDeliveryResponse(deliveryId = deliveryId))
    }

    @PostMapping("/{deliveryId}/complete")
    fun completeDelivery(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<CompleteDeliveryResponse> {
        deliveryService.completeDelivery(DeliveryId(deliveryId))
        return ResponseEntity.ok(CompleteDeliveryResponse(deliveryId = deliveryId))
    }

    @PostMapping("/{deliveryId}/complete-return")
    fun completeReturn(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<CompleteReturnResponse> {
        deliveryService.completeReturn(DeliveryId(deliveryId))
        return ResponseEntity.ok(CompleteReturnResponse(deliveryId = deliveryId))
    }

    @PostMapping("/{deliveryId}/open-door")
    fun openDoor(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<OpenDoorResponse> {
        deliveryService.openDoor(DeliveryId(deliveryId))
        return ResponseEntity.ok(OpenDoorResponse(deliveryId = deliveryId))
    }

    @PostMapping("/{deliveryId}/cancel")
    fun cancelDelivery(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<CancelDeliveryResponse> {
        val requiresReturn = deliveryService.cancelDelivery(DeliveryId(deliveryId))
        return ResponseEntity.ok(CancelDeliveryResponse.of(deliveryId, requiresReturn))
    }

    @PostMapping("/{deliveryId}/unassign-robot")
    fun unassignRobot(
        @PathVariable deliveryId: Long,
    ): ResponseEntity<UnassignRobotResponse> {
        deliveryService.unassignRobot(DeliveryId(deliveryId))
        return ResponseEntity.ok(UnassignRobotResponse(deliveryId = deliveryId))
    }

    @PostMapping("/{deliveryId}/reassign-robot")
    fun reassignRobot(
        @PathVariable deliveryId: Long,
        @RequestBody request: ReassignRobotRequest,
    ): ResponseEntity<ReassignRobotResponse> {
        val previousRobotId =
            deliveryService.reassignRobot(
                DeliveryId(deliveryId),
                RobotId(request.newRobotId),
            )

        return ResponseEntity.ok(
            ReassignRobotResponse(
                deliveryId = deliveryId,
                previousRobotId = previousRobotId.value,
                newRobotId = request.newRobotId,
            ),
        )
    }
}
