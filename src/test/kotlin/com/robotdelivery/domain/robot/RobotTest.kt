package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.robot.event.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Robot 테스트")
class RobotTest {
    private lateinit var defaultLocation: Location

    @BeforeEach
    fun setUp() {
        defaultLocation = Location(latitude = 37.5665, longitude = 126.9780)
    }

    private fun createRobot(
        id: Long = 1L,
        name: String = "로봇-001",
        status: RobotStatus = RobotStatus.OFF_DUTY,
        battery: Int = 100,
        location: Location = defaultLocation,
        currentDeliveryId: DeliveryId? = null,
    ): Robot =
        Robot(
            id = id,
            name = name,
            status = status,
            battery = battery,
            location = location,
            currentDeliveryId = currentDeliveryId,
        )

    @Nested
    @DisplayName("생성 테스트")
    inner class CreationTest {
        @Test
        @DisplayName("로봇을 생성하면 OFF_DUTY 상태이다")
        fun `로봇을 생성하면 OFF_DUTY 상태이다`() {
            val robot = createRobot()

            assertEquals(RobotStatus.OFF_DUTY, robot.status)
            assertEquals(100, robot.battery)
            assertNull(robot.currentDeliveryId)
        }

        @Test
        @DisplayName("로봇 ID를 조회할 수 있다")
        fun `로봇 ID를 조회할 수 있다`() {
            val robot = createRobot(id = 5L)

            assertEquals(5L, robot.getRobotId().value)
        }
    }

    @Nested
    @DisplayName("출근 테스트")
    inner class StartDutyTest {
        @Test
        @DisplayName("OFF_DUTY 상태에서 출근할 수 있다")
        fun `OFF_DUTY 상태에서 출근할 수 있다`() {
            val robot = createRobot()

            robot.startDuty()

            assertEquals(RobotStatus.READY, robot.status)
        }

        @Test
        @DisplayName("출근 시 RobotStartedDutyEvent와 RobotBecameAvailableEvent가 발생한다")
        fun `출근 시 RobotStartedDutyEvent와 RobotBecameAvailableEvent가 발생한다`() {
            val robot = createRobot()

            robot.startDuty()
            val events = robot.pullDomainEvents()

            assertEquals(2, events.size)
            assertTrue(events[0] is RobotStartedDutyEvent)
            assertTrue(events[1] is RobotBecameAvailableEvent)
        }
    }

    @Nested
    @DisplayName("퇴근 테스트")
    inner class EndDutyTest {
        @Test
        @DisplayName("READY 상태에서 퇴근할 수 있다")
        fun `READY 상태에서 퇴근할 수 있다`() {
            val robot = createRobot(status = RobotStatus.READY)

            robot.endDuty()

            assertEquals(RobotStatus.OFF_DUTY, robot.status)
        }

        @Test
        @DisplayName("퇴근 시 RobotEndedDutyEvent가 발생한다")
        fun `퇴근 시 RobotEndedDutyEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)

            robot.endDuty()
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            assertTrue(events[0] is RobotEndedDutyEvent)
        }

        @Test
        @DisplayName("배달 수행 중에는 퇴근할 수 없다")
        fun `배달 수행 중에는 퇴근할 수 없다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            val exception =
                assertThrows<IllegalArgumentException> {
                    robot.endDuty()
                }
            assertTrue(
                exception.message!!.contains("퇴근할 수 없는 상태입니다") ||
                    exception.message!!.contains("배달 수행 중에는 퇴근할 수 없습니다"),
            )
        }

        @Test
        @DisplayName("OFF_DUTY 상태에서 퇴근하면 예외가 발생한다")
        fun `OFF_DUTY 상태에서 퇴근하면 예외가 발생한다`() {
            val robot = createRobot()

            val exception =
                assertThrows<IllegalArgumentException> {
                    robot.endDuty()
                }
            assertTrue(exception.message!!.contains("퇴근할 수 없는 상태입니다"))
        }
    }

    @Nested
    @DisplayName("배달 할당 테스트")
    inner class AssignDeliveryTest {
        @Test
        @DisplayName("READY 상태에서 배달을 할당받을 수 있다")
        fun `READY 상태에서 배달을 할당받을 수 있다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val deliveryId = DeliveryId(1L)

            robot.assignDelivery(deliveryId)

            assertEquals(RobotStatus.BUSY, robot.status)
            assertEquals(deliveryId, robot.currentDeliveryId)
        }

        @Test
        @DisplayName("배달 할당 시 RobotAssignedToDeliveryEvent가 발생한다")
        fun `배달 할당 시 RobotAssignedToDeliveryEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val deliveryId = DeliveryId(1L)

            robot.assignDelivery(deliveryId)
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            val event = events[0] as RobotAssignedToDeliveryEvent
            assertEquals(deliveryId, event.deliveryId)
        }

        @Test
        @DisplayName("OFF_DUTY 상태에서 배달을 할당받으면 예외가 발생한다")
        fun `OFF_DUTY 상태에서 배달을 할당받으면 예외가 발생한다`() {
            val robot = createRobot()

            val exception =
                assertThrows<IllegalArgumentException> {
                    robot.assignDelivery(DeliveryId(1L))
                }
            assertTrue(exception.message!!.contains("배달을 받을 수 없는 상태입니다"))
        }

        @Test
        @DisplayName("이미 배달 수행 중이면 새 배달을 할당받을 수 없다")
        fun `이미 배달 수행 중이면 새 배달을 할당받을 수 없다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            val exception =
                assertThrows<IllegalArgumentException> {
                    robot.assignDelivery(DeliveryId(2L))
                }
            assertTrue(
                exception.message!!.contains("배달을 받을 수 없는 상태입니다") ||
                    exception.message!!.contains("이미 다른 배달을 수행 중입니다"),
            )
        }
    }

    @Nested
    @DisplayName("배달 완료 테스트")
    inner class CompleteDeliveryTest {
        @Test
        @DisplayName("BUSY 상태에서 배달을 완료할 수 있다")
        fun `BUSY 상태에서 배달을 완료할 수 있다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            robot.completeDelivery()

            assertEquals(RobotStatus.READY, robot.status)
            assertNull(robot.currentDeliveryId)
        }

        @Test
        @DisplayName("배달 완료 시 RobotBecameAvailableEvent가 발생한다")
        fun `배달 완료 시 RobotBecameAvailableEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            robot.completeDelivery()
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            assertTrue(events[0] is RobotBecameAvailableEvent)
        }

        @Test
        @DisplayName("READY 상태에서 배달 완료하면 예외가 발생한다")
        fun `READY 상태에서 배달 완료하면 예외가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)

            val exception =
                assertThrows<IllegalArgumentException> {
                    robot.completeDelivery()
                }
            assertTrue(exception.message!!.contains("배달 수행 중이 아닙니다"))
        }
    }

    @Nested
    @DisplayName("배터리 업데이트 테스트")
    inner class UpdateBatteryTest {
        @Test
        @DisplayName("유효한 배터리 값으로 업데이트할 수 있다")
        fun `유효한 배터리 값으로 업데이트할 수 있다`() {
            val robot = createRobot()

            robot.updateBattery(50)

            assertEquals(50, robot.battery)
        }

        @Test
        @DisplayName("배터리 경계값으로 업데이트할 수 있다")
        fun `배터리 경계값으로 업데이트할 수 있다`() {
            val robot = createRobot()

            robot.updateBattery(0)
            assertEquals(0, robot.battery)

            robot.updateBattery(100)
            assertEquals(100, robot.battery)
        }

        @Test
        @DisplayName("배터리가 0 미만이면 예외가 발생한다")
        fun `배터리가 0 미만이면 예외가 발생한다`() {
            val robot = createRobot()

            val exception =
                assertThrows<IllegalArgumentException> {
                    robot.updateBattery(-1)
                }
            assertTrue(exception.message!!.contains("배터리는 0에서 100 사이여야 합니다"))
        }

        @Test
        @DisplayName("배터리가 100 초과이면 예외가 발생한다")
        fun `배터리가 100 초과이면 예외가 발생한다`() {
            val robot = createRobot()

            val exception =
                assertThrows<IllegalArgumentException> {
                    robot.updateBattery(101)
                }
            assertTrue(exception.message!!.contains("배터리는 0에서 100 사이여야 합니다"))
        }
    }

    @Nested
    @DisplayName("위치 업데이트 테스트")
    inner class UpdateLocationTest {
        @Test
        @DisplayName("새 위치로 업데이트할 수 있다")
        fun `새 위치로 업데이트할 수 있다`() {
            val robot = createRobot()
            val newLocation = Location(latitude = 37.4979, longitude = 127.0276)

            robot.updateLocation(newLocation)

            assertEquals(newLocation, robot.location)
        }

        @Test
        @DisplayName("위치 업데이트 시 RobotLocationUpdatedEvent가 발생한다")
        fun `위치 업데이트 시 RobotLocationUpdatedEvent가 발생한다`() {
            val robot = createRobot()
            val newLocation = Location(latitude = 37.4979, longitude = 127.0276)

            robot.updateLocation(newLocation)
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            val event = events[0] as RobotLocationUpdatedEvent
            assertEquals(newLocation, event.location)
        }
    }

    @Nested
    @DisplayName("가용성 테스트")
    inner class AvailabilityTest {
        @Test
        @DisplayName("READY 상태이고 배달이 없고 배터리가 20 이상이면 사용 가능하다")
        fun `READY 상태이고 배달이 없고 배터리가 20 이상이면 사용 가능하다`() {
            val robot = createRobot(status = RobotStatus.READY, battery = 50)

            assertTrue(robot.isAvailable())
        }

        @Test
        @DisplayName("배터리가 20 미만이면 사용 불가능하다")
        fun `배터리가 20 미만이면 사용 불가능하다`() {
            val robot = createRobot(status = RobotStatus.READY, battery = 19)

            assertFalse(robot.isAvailable())
        }

        @Test
        @DisplayName("배터리가 정확히 20이면 사용 가능하다")
        fun `배터리가 정확히 20이면 사용 가능하다`() {
            val robot = createRobot(status = RobotStatus.READY, battery = 20)

            assertTrue(robot.isAvailable())
        }

        @Test
        @DisplayName("BUSY 상태이면 사용 불가능하다")
        fun `BUSY 상태이면 사용 불가능하다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            assertFalse(robot.isAvailable())
        }

        @Test
        @DisplayName("OFF_DUTY 상태이면 사용 불가능하다")
        fun `OFF_DUTY 상태이면 사용 불가능하다`() {
            val robot = createRobot()

            assertFalse(robot.isAvailable())
        }

        @Test
        @DisplayName("현재 배달이 할당되어 있으면 사용 불가능하다")
        fun `현재 배달이 할당되어 있으면 사용 불가능하다`() {
            val robot = createRobot(status = RobotStatus.READY, currentDeliveryId = DeliveryId(1L))

            assertFalse(robot.isAvailable())
        }
    }
}
