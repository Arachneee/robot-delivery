package com.robotdelivery.application.command.delivery

import com.robotdelivery.application.command.Command
import com.robotdelivery.application.command.CommandHandler
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.Destination

data class CreateDeliveryCommand(
    val pickupAddress: String,
    val pickupAddressDetail: String?,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val deliveryAddress: String,
    val deliveryAddressDetail: String?,
    val deliveryLatitude: Double,
    val deliveryLongitude: Double,
    val phoneNumber: String,
) : Command

class CreateDeliveryCommandHandler(
    private val deliveryRepository: DeliveryRepository,
    private val eventPublisher: DomainEventPublisher,
) : CommandHandler<CreateDeliveryCommand, DeliveryId> {

    override fun handle(command: CreateDeliveryCommand): DeliveryId {
        val delivery = Delivery(
            pickupDestination = Destination(
                address = command.pickupAddress,
                addressDetail = command.pickupAddressDetail,
                location = Location(command.pickupLatitude, command.pickupLongitude),
            ),
            deliveryDestination = Destination(
                address = command.deliveryAddress,
                addressDetail = command.deliveryAddressDetail,
                location = Location(command.deliveryLatitude, command.deliveryLongitude),
            ),
            phoneNumber = command.phoneNumber,
        )

        val savedDelivery = deliveryRepository.save(delivery)

        eventPublisher.publishAll(savedDelivery.pullDomainEvents())

        return savedDelivery.getDeliveryId()
    }
}

