package com.robotdelivery.view.delivery

import com.robotdelivery.application.command.DeliveryService
import com.robotdelivery.domain.common.vo.DeliveryId
import com.robotdelivery.domain.common.vo.OrderNo
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.view.delivery.dto.CancelDeliveryResponse
import com.robotdelivery.view.delivery.dto.CompleteDeliveryResponse
import com.robotdelivery.view.delivery.dto.CompleteReturnResponse
import com.robotdelivery.view.delivery.dto.CreateAdditionalDeliveryRequest
import com.robotdelivery.view.delivery.dto.CreateAdditionalDeliveryResponse
import com.robotdelivery.view.delivery.dto.CreateDeliveryRequest
import com.robotdelivery.view.delivery.dto.CreateDeliveryResponse
import com.robotdelivery.view.delivery.dto.OpenDoorResponse
import com.robotdelivery.view.delivery.dto.ReassignRobotRequest
import com.robotdelivery.view.delivery.dto.ReassignRobotResponse
import com.robotdelivery.view.delivery.dto.StartDeliveryResponse
import com.robotdelivery.view.delivery.dto.UnassignRobotResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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

    @PostMapping("/additional")
    fun createAdditionalDelivery(
        @RequestBody request: CreateAdditionalDeliveryRequest,
    ): ResponseEntity<CreateAdditionalDeliveryResponse> {
        val deliveryId = deliveryService.createAdditionalDelivery(OrderNo(request.orderNo))

        val response =
            CreateAdditionalDeliveryResponse(
                deliveryId = deliveryId.value,
                orderNo = request.orderNo,
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
            ReassignRobotResponse.of(
                deliveryId = deliveryId,
                previousRobotId = previousRobotId?.value,
                newRobotId = request.newRobotId,
            ),
        )
    }
}
