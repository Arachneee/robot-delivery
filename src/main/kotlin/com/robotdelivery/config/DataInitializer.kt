package com.robotdelivery.config

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotIotState
import com.robotdelivery.domain.robot.RobotIotStateRepository
import com.robotdelivery.domain.robot.RobotRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val robotRepository: RobotRepository,
    private val iotStateRepository: RobotIotStateRepository,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (robotRepository.count() > 0) {
            return
        }

        val robotsWithLocations =
            listOf(
                "Robot-1" to Location(latitude = 37.5665, longitude = 126.9780),
                "Robot-2" to Location(latitude = 37.5700, longitude = 126.9820),
                "Robot-3" to Location(latitude = 37.5630, longitude = 126.9750),
            )

        robotsWithLocations.forEach { (name, location) ->
            val robot = Robot(name = name)
            val savedRobot = robotRepository.save(robot)

            iotStateRepository.save(
                RobotIotState(
                    robotId = savedRobot.getRobotId(),
                    location = location,
                    battery = 100,
                    doorOpen = false,
                    loadWeight = 0.0,
                ),
            )
        }
    }
}
