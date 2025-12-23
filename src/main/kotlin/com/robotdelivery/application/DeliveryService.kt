package com.robotdelivery.application

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.delivery.getById
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.getById
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
    private val robotRepository: RobotRepository,
    private val robotClient: RobotClient,
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

        return savedDelivery.getDeliveryId()
    }

    fun completeDelivery(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)

        val robotId =
            delivery.assignedRobotId
                ?: throw IllegalStateException("배차된 로봇이 없습니다.")

        val robot = robotRepository.getById(robotId)

        delivery.complete()
        robot.completeDelivery()

        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }

    fun openDoor(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)

        val robotId =
            delivery.assignedRobotId
                ?: throw IllegalStateException("배차된 로봇이 없습니다.")

        delivery.openDoor()
        deliveryRepository.save(delivery)

        robotClient.openDoor(robotId)
    }

    fun startDelivery(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)

        val robotId =
            delivery.assignedRobotId
                ?: throw IllegalStateException("배차된 로봇이 없습니다.")

        val robot = robotRepository.getById(robotId)

        delivery.startDelivery()
        robot.navigateTo(delivery.deliveryDestination.location)

        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }

    fun completeReturn(deliveryId: DeliveryId) {
        val delivery = deliveryRepository.getById(deliveryId)

        val robotId =
            delivery.assignedRobotId
                ?: throw IllegalStateException("배차된 로봇이 없습니다.")

        val robot = robotRepository.getById(robotId)

        delivery.completeReturn()
        robot.completeDelivery()

        deliveryRepository.save(delivery)
        robotRepository.save(robot)
    }

    fun cancelDelivery(deliveryId: DeliveryId): Boolean {
        val delivery = deliveryRepository.getById(deliveryId)

        val requiresReturn = delivery.status.requiresReturn()

        delivery.cancel()

        val robotId = delivery.assignedRobotId
        if (robotId != null) {
            val robot = robotRepository.getById(robotId)

            if (requiresReturn) {
                robot.navigateTo(delivery.pickupDestination.location)
            } else {
                robot.completeDelivery()
            }
            robotRepository.save(robot)
        }

        deliveryRepository.save(delivery)

        return requiresReturn
    }
}
