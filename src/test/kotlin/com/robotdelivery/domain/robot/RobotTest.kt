package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.robot.event.RobotArrivedAtDestinationEvent
import com.robotdelivery.domain.robot.event.RobotBecameAvailableEvent
import com.robotdelivery.domain.robot.event.RobotDestinationChangedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
        @DisplayName("출근 시 RobotBecameAvailableEvent가 발생한다")
        fun `출근 시 RobotBecameAvailableEvent가 발생한다`() {
            val robot = createRobot()

            robot.startDuty()
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            assertTrue(events[0] is RobotBecameAvailableEvent)
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
        @DisplayName("퇴근 시 이벤트가 발생하지 않는다")
        fun `퇴근 시 이벤트가 발생하지 않는다`() {
            val robot = createRobot(status = RobotStatus.READY)

            robot.endDuty()
            val events = robot.pullDomainEvents()

            assertTrue(events.isEmpty())
        }

        @Test
        @DisplayName("배달 수행 중에는 퇴근할 수 없다")
        fun `배달 수행 중에는 퇴근할 수 없다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            val exception =
                assertThrows<IllegalStateException> {
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
                assertThrows<IllegalStateException> {
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
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)

            robot.assignDelivery(deliveryId, pickupLocation)

            assertEquals(RobotStatus.BUSY, robot.status)
            assertEquals(deliveryId, robot.currentDeliveryId)
        }

        @Test
        @DisplayName("배달 할당 시 RobotDestinationChangedEvent가 발생한다")
        fun `배달 할당 시 RobotDestinationChangedEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val deliveryId = DeliveryId(1L)
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)

            robot.assignDelivery(deliveryId, pickupLocation)
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            val destinationChangedEvent = events[0] as RobotDestinationChangedEvent
            assertEquals(pickupLocation, destinationChangedEvent.destination)
        }

        @Test
        @DisplayName("OFF_DUTY 상태에서 배달을 할당받으면 예외가 발생한다")
        fun `OFF_DUTY 상태에서 배달을 할당받으면 예외가 발생한다`() {
            val robot = createRobot()
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)

            val exception =
                assertThrows<IllegalStateException> {
                    robot.assignDelivery(DeliveryId(1L), pickupLocation)
                }
            assertTrue(exception.message!!.contains("배달을 받을 수 없는 상태입니다"))
        }

        @Test
        @DisplayName("이미 배달 수행 중이면 새 배달을 할당받을 수 없다")
        fun `이미 배달 수행 중이면 새 배달을 할당받을 수 없다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)

            val exception =
                assertThrows<IllegalStateException> {
                    robot.assignDelivery(DeliveryId(2L), pickupLocation)
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
                assertThrows<IllegalStateException> {
                    robot.completeDelivery()
                }
            assertTrue(exception.message!!.contains("배달 수행 중이 아닙니다"))
        }
    }

    @Nested
    @DisplayName("배차 취소 테스트")
    inner class UnassignDeliveryTest {
        @Test
        @DisplayName("BUSY 상태에서 배차를 취소할 수 있다")
        fun `BUSY 상태에서 배차를 취소할 수 있다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            robot.unassignDelivery()

            assertEquals(RobotStatus.READY, robot.status)
            assertNull(robot.currentDeliveryId)
        }

        @Test
        @DisplayName("배차 취소 시 destination이 null로 설정된다")
        fun `배차 취소 시 destination이 null로 설정된다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            robot.assignDelivery(DeliveryId(1L), pickupLocation)

            robot.unassignDelivery()

            assertNull(robot.destination)
        }

        @Test
        @DisplayName("배차 취소 시 RobotBecameAvailableEvent가 발생한다")
        fun `배차 취소 시 RobotBecameAvailableEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))

            robot.unassignDelivery()
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            assertTrue(events[0] is RobotBecameAvailableEvent)
        }

        @Test
        @DisplayName("READY 상태에서 배차 취소하면 예외가 발생한다")
        fun `READY 상태에서 배차 취소하면 예외가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)

            val exception =
                assertThrows<IllegalStateException> {
                    robot.unassignDelivery()
                }
            assertTrue(exception.message!!.contains("배달 수행 중이 아닙니다"))
        }

        @Test
        @DisplayName("할당된 배달이 없으면 예외가 발생한다")
        fun `할당된 배달이 없으면 예외가 발생한다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = null)

            val exception =
                assertThrows<IllegalStateException> {
                    robot.unassignDelivery()
                }
            assertTrue(exception.message!!.contains("할당된 배달이 없습니다"))
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
        @DisplayName("목적지가 없으면 도착 이벤트가 발생하지 않는다")
        fun `목적지가 없으면 도착 이벤트가 발생하지 않는다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val newLocation = Location(latitude = 37.4979, longitude = 127.0276)

            robot.updateLocation(newLocation)
            val events = robot.pullDomainEvents()

            assertTrue(events.isEmpty())
        }

        @Test
        @DisplayName("목적지에 도착하면 RobotArrivedAtDestinationEvent가 발생한다")
        fun `목적지에 도착하면 RobotArrivedAtDestinationEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            robot.assignDelivery(DeliveryId(1L), pickupLocation)
            robot.pullDomainEvents() // 배달 할당 이벤트 제거

            robot.updateLocation(pickupLocation)
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            val event = events[0] as RobotArrivedAtDestinationEvent
            assertEquals(pickupLocation, event.destination)
        }

        @Test
        @DisplayName("목적지에서 5m 이내이면 도착으로 판단한다")
        fun `목적지에서 5m 이내이면 도착으로 판단한다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            robot.assignDelivery(DeliveryId(1L), pickupLocation)
            robot.pullDomainEvents()

            // 약 3m 떨어진 위치 (위도 0.00003도 ≈ 약 3.3m)
            val nearbyLocation = Location(latitude = 37.50003, longitude = 127.0000)
            robot.updateLocation(nearbyLocation)
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            assertTrue(events[0] is RobotArrivedAtDestinationEvent)
        }

        @Test
        @DisplayName("목적지에서 5m 초과이면 도착 이벤트가 발생하지 않는다")
        fun `목적지에서 5m 초과이면 도착 이벤트가 발생하지 않는다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            robot.assignDelivery(DeliveryId(1L), pickupLocation)
            robot.pullDomainEvents()

            // 약 10m 떨어진 위치 (위도 0.0001도 ≈ 약 11m)
            val farLocation = Location(latitude = 37.5001, longitude = 127.0000)
            robot.updateLocation(farLocation)
            val events = robot.pullDomainEvents()

            assertTrue(events.isEmpty())
        }

        @Test
        @DisplayName("도착 이벤트 발행 후 destination이 null로 설정된다")
        fun `도착 이벤트 발행 후 destination이 null로 설정된다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            robot.assignDelivery(DeliveryId(1L), pickupLocation)
            robot.pullDomainEvents()

            robot.updateLocation(pickupLocation)

            assertNull(robot.destination)
        }

        @Test
        @DisplayName("도착 이벤트 발행 후 추가 위치 업데이트에서 중복 발행하지 않는다")
        fun `도착 이벤트 발행 후 추가 위치 업데이트에서 중복 발행하지 않는다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            robot.assignDelivery(DeliveryId(1L), pickupLocation)
            robot.pullDomainEvents()

            // 첫 번째 도착
            robot.updateLocation(pickupLocation)
            val firstEvents = robot.pullDomainEvents()
            assertEquals(1, firstEvents.size)

            // 두 번째 위치 업데이트 (같은 위치)
            robot.updateLocation(pickupLocation)
            val secondEvents = robot.pullDomainEvents()

            assertTrue(secondEvents.isEmpty())
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

    @Nested
    @DisplayName("목적지 이동 테스트")
    inner class NavigateToTest {
        @Test
        @DisplayName("BUSY 상태에서 목적지를 설정할 수 있다")
        fun `BUSY 상태에서 목적지를 설정할 수 있다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))
            val destination = Location(latitude = 37.5000, longitude = 127.0000)

            robot.navigateTo(destination)

            assertEquals(destination, robot.destination)
        }

        @Test
        @DisplayName("목적지 설정 시 RobotDestinationChangedEvent가 발생한다")
        fun `목적지 설정 시 RobotDestinationChangedEvent가 발생한다`() {
            val robot = createRobot(status = RobotStatus.BUSY, currentDeliveryId = DeliveryId(1L))
            val destination = Location(latitude = 37.5000, longitude = 127.0000)

            robot.navigateTo(destination)
            val events = robot.pullDomainEvents()

            assertEquals(1, events.size)
            val event = events[0] as RobotDestinationChangedEvent
            assertEquals(robot.getRobotId(), event.robotId)
            assertEquals(destination, event.destination)
        }

        @Test
        @DisplayName("READY 상태에서 목적지 설정하면 예외가 발생한다")
        fun `READY 상태에서 목적지 설정하면 예외가 발생한다`() {
            val robot = createRobot(status = RobotStatus.READY)
            val destination = Location(latitude = 37.5000, longitude = 127.0000)

            val exception =
                assertThrows<IllegalStateException> {
                    robot.navigateTo(destination)
                }
            assertTrue(exception.message!!.contains("배달 수행 중이 아닙니다"))
        }

        @Test
        @DisplayName("OFF_DUTY 상태에서 목적지 설정하면 예외가 발생한다")
        fun `OFF_DUTY 상태에서 목적지 설정하면 예외가 발생한다`() {
            val robot = createRobot()
            val destination = Location(latitude = 37.5000, longitude = 127.0000)

            val exception =
                assertThrows<IllegalStateException> {
                    robot.navigateTo(destination)
                }
            assertTrue(exception.message!!.contains("배달 수행 중이 아닙니다"))
        }
    }
}
