package com.robotdelivery.application.eventhandler

import com.robotdelivery.config.IntegrationTestSupport
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotIotStateRepository
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.infrastructure.event.external.RobotIotUpdatedEvent
import com.robotdelivery.infrastructure.persistence.InMemoryRobotIotStateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("RobotIotEventHandler 테스트")
class RobotIotEventHandlerTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var robotRepository: RobotRepository

    @Autowired
    private lateinit var iotStateRepository: RobotIotStateRepository

    @Autowired
    private lateinit var eventHandler: RobotIotEventHandler

    @BeforeEach
    fun setUp() {
        robotRepository.deleteAll()
        (iotStateRepository as InMemoryRobotIotStateRepository).deleteAll()
    }

    private fun saveRobot(name: String = "로봇-1"): Robot {
        val robot = Robot(name = name, status = RobotStatus.READY)
        return robotRepository.saveAndFlush(robot)
    }

    private fun createEvent(
        robotId: RobotId,
        location: Location = Location(latitude = 37.5665, longitude = 126.9780),
        battery: Int = 100,
        doorOpen: Boolean = false,
        loadWeight: Double = 0.0,
    ) = RobotIotUpdatedEvent(
        robotId = robotId,
        location = location,
        battery = battery,
        doorOpen = doorOpen,
        loadWeight = loadWeight,
    )

    @Nested
    @DisplayName("RobotIotUpdatedEvent 핸들러 테스트")
    inner class HandleRobotIotUpdatedEventTest {
        @Test
        @DisplayName("IoT 상태가 저장된다")
        fun `IoT 상태가 저장된다`() {
            val robot = saveRobot()
            val newLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val event =
                createEvent(
                    robotId = robot.getRobotId(),
                    location = newLocation,
                    battery = 80,
                    doorOpen = true,
                    loadWeight = 5.5,
                )

            eventHandler.handle(event)

            val iotState = iotStateRepository.findById(robot.getRobotId())!!
            assertThat(iotState.location).isEqualTo(newLocation)
            assertThat(iotState.battery).isEqualTo(80)
            assertThat(iotState.doorOpen).isTrue()
            assertThat(iotState.loadWeight).isEqualTo(5.5)
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 IoT 상태만 저장된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 IoT 상태만 저장된다`() {
            val nonExistentRobotId = RobotId(99999L)
            val event = createEvent(robotId = nonExistentRobotId)

            eventHandler.handle(event)

            val iotState = iotStateRepository.findById(nonExistentRobotId)
            assertThat(iotState).isNotNull
        }

        @Test
        @DisplayName("같은 값으로 업데이트해도 저장된다")
        fun `같은 값으로 업데이트해도 저장된다`() {
            val robot = saveRobot()
            val location = Location(latitude = 37.5665, longitude = 126.9780)
            val event =
                createEvent(
                    robotId = robot.getRobotId(),
                    location = location,
                    battery = 100,
                    doorOpen = false,
                    loadWeight = 0.0,
                )

            eventHandler.handle(event)

            val iotState = iotStateRepository.findById(robot.getRobotId())!!
            assertThat(iotState.location).isEqualTo(location)
        }

        @Test
        @DisplayName("여러 번 IoT 이벤트로 업데이트가 가능하다")
        fun `여러 번 IoT 이벤트로 업데이트가 가능하다`() {
            val robot = saveRobot()
            val location1 = Location(latitude = 37.5000, longitude = 127.0000)
            val location2 = Location(latitude = 37.6000, longitude = 127.1000)

            eventHandler.handle(
                createEvent(
                    robotId = robot.getRobotId(),
                    location = location1,
                    battery = 90,
                    doorOpen = false,
                    loadWeight = 1.0,
                ),
            )
            var iotState = iotStateRepository.findById(robot.getRobotId())!!
            assertThat(iotState.location).isEqualTo(location1)
            assertThat(iotState.battery).isEqualTo(90)

            eventHandler.handle(
                createEvent(
                    robotId = robot.getRobotId(),
                    location = location2,
                    battery = 85,
                    doorOpen = true,
                    loadWeight = 2.5,
                ),
            )
            iotState = iotStateRepository.findById(robot.getRobotId())!!
            assertThat(iotState.location).isEqualTo(location2)
            assertThat(iotState.battery).isEqualTo(85)
            assertThat(iotState.doorOpen).isTrue()
            assertThat(iotState.loadWeight).isEqualTo(2.5)
        }
    }
}
