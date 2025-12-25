package com.robotdelivery.application.query

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.RobotIotState
import com.robotdelivery.domain.robot.RobotIotStateRepository
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.findById
import org.springframework.stereotype.Service

@Service
class RobotQueryService(
    private val robotRepository: RobotRepository,
    private val iotStateRepository: RobotIotStateRepository,
) {
    fun updateIotState(iotState: RobotIotState) {
        iotStateRepository.save(iotState)

        val robot = robotRepository.findById(iotState.robotId) ?: return

        if (robot.destination != null) {
            robot.updateDrivingStatus(iotState.location)
            robotRepository.save(robot)
        }
    }

    fun findNearestAvailableRobotId(
        pickupLocation: Location,
        minimumBattery: Int = 20,
    ): RobotId? {
        val iotStates =
            iotStateRepository
                .findAll()
                .filter { it.hasSufficientBattery(minimumBattery) }
                .ifEmpty { return null }

        val availableRobotIds =
            robotRepository
                .findAllById(iotStates.map { it.robotId.value })
                .filter { it.isAvailableForDelivery() }
                .map { it.getRobotId() }
                .toSet()

        return iotStates
            .filter { it.robotId in availableRobotIds }
            .minByOrNull { it.distanceTo(pickupLocation) }
            ?.robotId
    }

    fun findIotState(robotId: RobotId): RobotIotState? = iotStateRepository.findById(robotId)
}
