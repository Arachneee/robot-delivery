package com.robotdelivery.application.eventhandler

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.infrastructure.event.external.RobotLocationUpdatedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("RobotLocationEventHandler 테스트")
class RobotLocationEventHandlerTest {
    @Mock
    private lateinit var robotRepository: RobotRepository

    private lateinit var eventHandler: RobotLocationEventHandler

    @BeforeEach
    fun setUp() {
        eventHandler = RobotLocationEventHandler(robotRepository)
    }

    private fun createRobot(
        id: Long = 1L,
        name: String = "로봇-1",
        location: Location = Location(latitude = 37.5665, longitude = 126.9780),
    ): Robot =
        Robot(
            id = id,
            name = name,
            status = RobotStatus.READY,
            battery = 100,
            location = location,
        )

    @Nested
    @DisplayName("RobotLocationUpdatedEvent 핸들러 테스트")
    inner class HandleRobotLocationUpdatedEventTest {
        @Test
        @DisplayName("로봇 위치가 업데이트된다")
        fun `로봇 위치가 업데이트된다`() {
            val robot = createRobot()
            val newLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val event =
                RobotLocationUpdatedEvent(
                    robotId = robot.getRobotId(),
                    location = newLocation,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)

            eventHandler.handle(event)

            assertEquals(newLocation, robot.location)
            verify(robotRepository).save(robot)
        }

        @Test
        @DisplayName("존재하지 않는 로봇 ID로 이벤트가 오면 무시된다")
        fun `존재하지 않는 로봇 ID로 이벤트가 오면 무시된다`() {
            val event =
                RobotLocationUpdatedEvent(
                    robotId = RobotId(99999L),
                    location = Location(37.5665, 126.9780),
                )

            whenever(robotRepository.findById(RobotId(99999L))).thenReturn(null)

            eventHandler.handle(event)

            verify(robotRepository, never()).save(any())
        }

        @Test
        @DisplayName("같은 위치로 업데이트해도 저장된다")
        fun `같은 위치로 업데이트해도 저장된다`() {
            val location = Location(latitude = 37.5665, longitude = 126.9780)
            val robot = createRobot(location = location)
            val event =
                RobotLocationUpdatedEvent(
                    robotId = robot.getRobotId(),
                    location = location,
                )

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)

            eventHandler.handle(event)

            assertEquals(location, robot.location)
            verify(robotRepository).save(robot)
        }

        @Test
        @DisplayName("여러 번 위치 업데이트가 가능하다")
        fun `여러 번 위치 업데이트가 가능하다`() {
            val robot = createRobot()
            val location1 = Location(latitude = 37.5000, longitude = 127.0000)
            val location2 = Location(latitude = 37.6000, longitude = 127.1000)

            whenever(robotRepository.findById(robot.getRobotId())).thenReturn(robot)

            eventHandler.handle(
                RobotLocationUpdatedEvent(
                    robotId = robot.getRobotId(),
                    location = location1,
                ),
            )
            assertEquals(location1, robot.location)

            eventHandler.handle(
                RobotLocationUpdatedEvent(
                    robotId = robot.getRobotId(),
                    location = location2,
                ),
            )
            assertEquals(location2, robot.location)
        }
    }
}

