package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.robot.vo.AvailableRobotCandidate

class RobotAvailabilityService(
    private val robotRepository: RobotRepository,
    private val iotStateRepository: RobotIotStateRepository,
) {
    fun findAvailableCandidatesSortedByDistance(
        pickupLocation: Location,
        minimumBattery: Int = 20,
    ): List<AvailableRobotCandidate> {
        val iotStates =
            iotStateRepository
                .findAll()
                .asSequence()
                .filter { it.hasSufficientBattery(minimumBattery) }
                .toList()
                .ifEmpty { return emptyList() }

        val availableRobots =
            robotRepository
                .findAllById(iotStates.map { it.robotId.value })
                .asSequence()
                .filter { it.isAvailableForDelivery() }
                .associateBy { it.getRobotId() }

        return iotStates
            .asSequence()
            .mapNotNull { iotState ->
                availableRobots[iotState.robotId]?.let { robot ->
                    AvailableRobotCandidate(robot, iotState)
                }
            }.sortedBy { it.iotState.distanceTo(pickupLocation) }
            .toList()
    }

    fun findIotState(robotId: RobotId): RobotIotState? = iotStateRepository.findById(robotId)
}
