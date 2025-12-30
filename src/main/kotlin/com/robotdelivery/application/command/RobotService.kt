package com.robotdelivery.application.command

import com.robotdelivery.domain.robot.RobotIotState
import com.robotdelivery.domain.robot.RobotIotStateRepository
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.findById
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RobotService(
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
}
