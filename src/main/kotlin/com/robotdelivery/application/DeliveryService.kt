package com.robotdelivery.application

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.DomainEventPublisher
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.robot.RobotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val eventPublisher: DomainEventPublisher,
) {
    fun createDelivery(
        pickupAddress: String,
        pickupAddressDetail: String?,
        pickupLatitude: Double,
        pickupLongitude: Double,
        deliveryAddress: String,
        deliveryAddressDetail: String?,
        deliveryLatitude: Double,
        deliveryLongitude: Double,
        phoneNumber: String,
    ): DeliveryId {
        val delivery =
            Delivery(
                pickupDestination =
                    Destination(
                        address = pickupAddress,
                        addressDetail = pickupAddressDetail,
                        location = Location(pickupLatitude, pickupLongitude),
                    ),
                deliveryDestination =
                    Destination(
                        address = deliveryAddress,
                        addressDetail = deliveryAddressDetail,
                        location = Location(deliveryLatitude, deliveryLongitude),
                    ),
                phoneNumber = phoneNumber,
            )

        val savedDelivery = deliveryRepository.saveAndFlush(delivery)

        eventPublisher.publishAll(savedDelivery.pullDomainEvents())

        return savedDelivery.getDeliveryId()
    }

    fun completeDelivery(deliveryId: DeliveryId) {
        val delivery =
            deliveryRepository.findById(deliveryId)
                ?: throw IllegalArgumentException("배달을 찾을 수 없습니다: $deliveryId")

        val robotId =
            delivery.assignedRobotId
                ?: throw IllegalStateException("배차된 로봇이 없습니다.")

        val robot =
            robotRepository.findById(robotId)
                ?: throw IllegalStateException("로봇을 찾을 수 없습니다: $robotId")

        delivery.complete()
        robot.completeDelivery()

        deliveryRepository.save(delivery)
        robotRepository.save(robot)

        eventPublisher.publishAll(delivery.pullDomainEvents())
        eventPublisher.publishAll(robot.pullDomainEvents())
    }
}
