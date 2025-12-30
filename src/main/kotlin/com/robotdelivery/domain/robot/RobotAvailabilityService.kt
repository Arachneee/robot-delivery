package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId

class RobotAvailabilityService(
    private val robotRepository: RobotRepository,
    private val iotStateRepository: RobotIotStateRepository,
) {
    fun findNearestAvailableRobotId(
        pickupLocation: Location,
        minimumBattery: Int = 20,
    ): RobotId? {
        val iotStates =
            iotStateRepository
                .findAll()
                .asSequence()
                .filter { it.hasSufficientBattery(minimumBattery) }
                .toList()
                .ifEmpty { return null }

        val availableRobotIds =
            robotRepository
                .findAllById(iotStates.map { it.robotId.value })
                .asSequence()
                .filter { it.isAvailableForDelivery() }
                .map { it.getRobotId() }
                .toSet()

        return iotStates
            .asSequence()
            .filter { it.robotId in availableRobotIds }
            .minByOrNull { it.distanceTo(pickupLocation) }
            ?.robotId
    }

    fun findIotState(robotId: RobotId): RobotIotState? = iotStateRepository.findById(robotId)
}
