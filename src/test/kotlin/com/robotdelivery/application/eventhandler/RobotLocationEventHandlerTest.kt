package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.infrastructure.event.external.RobotLocationUpdatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.robotdelivery.config.TestAsyncConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Import(TestAsyncConfig::class)
@ActiveProfiles("test")
@Transactional
@DisplayName("RobotLocationEventHandler 테스트")
class RobotLocationEventHandlerTest {
    @Autowired
    private lateinit var robotRepository: RobotRepository

    @Autowired
    private lateinit var eventHandler: RobotLocationEventHandler

    @BeforeEach
    fun setUp() {
        robotRepository.deleteAll()
    }

    private fun saveRobot(
        name: String = "로봇-1",
        location: Location = Location(latitude = 37.5665, longitude = 126.9780),
    ): Robot {
        val robot =
            Robot(
                name = name,
                status = RobotStatus.READY,
                battery = 100,
                location = location,
            )
        return robotRepository.saveAndFlush(robot)
    }

    @Nested
    @DisplayName("RobotLocationUpdatedEvent 핸들러 테스트")
    inner class HandleRobotLocationUpdatedEventTest {
        @Test
        @DisplayName("로봇 위치가 업데이트된다")
        fun `로봇 위치가 업데이트된다`() {
            val robot = saveRobot()
            val newLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val event =
                RobotLocationUpdatedEvent(
                    robotId = robot.getRobotId(),
                    location = newLocation,
                )

            eventHandler.handle(event)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.location).isEqualTo(newLocation)
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 무시된다`() {
            val event =
                RobotLocationUpdatedEvent(
                    robotId = RobotId(99999L),
                    location = Location(37.5665, 126.9780),
                )

            eventHandler.handle(event)
        }

        @Test
        @DisplayName("같은 위치로 업데이트해도 저장된다")
        fun `같은 위치로 업데이트해도 저장된다`() {
            val location = Location(latitude = 37.5665, longitude = 126.9780)
            val robot = saveRobot(location = location)
            val event =
                RobotLocationUpdatedEvent(
                    robotId = robot.getRobotId(),
                    location = location,
                )

            eventHandler.handle(event)

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.location).isEqualTo(location)
        }

        @Test
        @DisplayName("여러 번 위치 업데이트가 가능하다")
        fun `여러 번 위치 업데이트가 가능하다`() {
            val robot = saveRobot()
            val location1 = Location(latitude = 37.5000, longitude = 127.0000)
            val location2 = Location(latitude = 37.6000, longitude = 127.1000)

            eventHandler.handle(
                RobotLocationUpdatedEvent(
                    robotId = robot.getRobotId(),
                    location = location1,
                ),
            )
            var updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.location).isEqualTo(location1)

            eventHandler.handle(
                RobotLocationUpdatedEvent(
                    robotId = robot.getRobotId(),
                    location = location2,
                ),
            )
            updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.location).isEqualTo(location2)
        }
    }
}
