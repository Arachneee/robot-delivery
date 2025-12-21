package com.robotdelivery.config

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val robotRepository: RobotRepository,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (robotRepository.count() > 0) {
            return
        }

        val robots =
            listOf(
                Robot(
                    name = "Robot-1",
                    location = Location(latitude = 37.5665, longitude = 126.9780),
                ),
                Robot(
                    name = "Robot-2",
                    location = Location(latitude = 37.5700, longitude = 126.9820),
                ),
                Robot(
                    name = "Robot-3",
                    location = Location(latitude = 37.5630, longitude = 126.9750),
                ),
            )

        robotRepository.saveAll(robots)
    }
}
