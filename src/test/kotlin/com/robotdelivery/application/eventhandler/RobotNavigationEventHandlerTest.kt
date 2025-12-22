package com.robotdelivery.application.eventhandler

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.event.RobotDestinationChangedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
@DisplayName("RobotNavigationEventHandler 테스트")
class RobotNavigationEventHandlerTest {
    @Mock
    private lateinit var robotClient: RobotClient

    private lateinit var eventHandler: RobotNavigationEventHandler

    @BeforeEach
    fun setUp() {
        eventHandler = RobotNavigationEventHandler(robotClient)
    }

    @Nested
    @DisplayName("RobotDestinationChangedEvent 핸들러 테스트")
    inner class HandleRobotDestinationChangedEventTest {
        @Test
        @DisplayName("목적지 변경 이벤트 발생 시 RobotClient.navigateTo가 호출된다")
        fun `목적지 변경 이벤트 발생 시 RobotClient navigateTo가 호출된다`() {
            val robotId = RobotId(1L)
            val destination = Location(latitude = 37.5665, longitude = 126.9780)
            val event =
                RobotDestinationChangedEvent(
                    robotId = robotId,
                    destination = destination,
                )

            eventHandler.handle(event)

            verify(robotClient).navigateTo(robotId, destination)
        }

        @Test
        @DisplayName("다른 목적지로 변경 시 새로운 목적지로 navigateTo가 호출된다")
        fun `다른 목적지로 변경 시 새로운 목적지로 navigateTo가 호출된다`() {
            val robotId = RobotId(1L)
            val destination1 = Location(latitude = 37.5665, longitude = 126.9780)
            val destination2 = Location(latitude = 37.4979, longitude = 127.0276)

            eventHandler.handle(
                RobotDestinationChangedEvent(
                    robotId = robotId,
                    destination = destination1,
                ),
            )
            verify(robotClient).navigateTo(robotId, destination1)

            eventHandler.handle(
                RobotDestinationChangedEvent(
                    robotId = robotId,
                    destination = destination2,
                ),
            )
            verify(robotClient).navigateTo(robotId, destination2)
        }

        @Test
        @DisplayName("여러 로봇에 대해 각각 navigateTo가 호출된다")
        fun `여러 로봇에 대해 각각 navigateTo가 호출된다`() {
            val robotId1 = RobotId(1L)
            val robotId2 = RobotId(2L)
            val destination = Location(latitude = 37.5665, longitude = 126.9780)

            eventHandler.handle(
                RobotDestinationChangedEvent(
                    robotId = robotId1,
                    destination = destination,
                ),
            )
            eventHandler.handle(
                RobotDestinationChangedEvent(
                    robotId = robotId2,
                    destination = destination,
                ),
            )

            verify(robotClient).navigateTo(robotId1, destination)
            verify(robotClient).navigateTo(robotId2, destination)
        }
    }
}

