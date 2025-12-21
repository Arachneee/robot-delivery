package com.robotdelivery.domain.delivery

import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("DeliveryAssignmentService 테스트")
class DeliveryAssignmentServiceTest {
    @Autowired
    private lateinit var robotRepository: RobotRepository

    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    private lateinit var assignmentService: DeliveryAssignmentService

    @BeforeEach
    fun setUp() {
        robotRepository.deleteAll()
        deliveryRepository.deleteAll()
        assignmentService = DeliveryAssignmentService(robotRepository, deliveryRepository)
    }

    private fun createDelivery(pickupLocation: Location = Location(latitude = 37.5665, longitude = 126.9780)): Delivery =
        deliveryRepository.save(
            Delivery(
                pickupDestination =
                    Destination(
                        address = "서울시 중구 세종대로 110",
                        location = pickupLocation,
                    ),
                deliveryDestination =
                    Destination(
                        address = "서울시 강남구 테헤란로 1",
                        location = Location(latitude = 37.4979, longitude = 127.0276),
                    ),
                phoneNumber = "010-1234-5678",
            ),
        )

    private fun createRobot(
        name: String,
        status: RobotStatus = RobotStatus.READY,
        battery: Int = 100,
        location: Location,
    ): Robot =
        robotRepository.save(
            Robot(
                name = name,
                status = status,
                battery = battery,
                location = location,
            ),
        )

    @Nested
    @DisplayName("가장 가까운 로봇 배정 테스트")
    inner class AssignNearestRobotTest {
        @Test
        @DisplayName("가장 가까운 로봇이 배정된다")
        fun `가장 가까운 로봇이 배정된다`() {
            // 픽업 위치: 서울시청
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = createDelivery(pickupLocation = pickupLocation)

            // 강남역 근처 로봇 (더 멀리)
            val farRobot =
                createRobot(
                    name = "로봇-far",
                    location = Location(latitude = 37.4979, longitude = 127.0276),
                )
            // 시청 근처 로봇 (더 가까이)
            val nearRobot =
                createRobot(
                    name = "로봇-near",
                    location = Location(latitude = 37.5660, longitude = 126.9770),
                )

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertNotNull(assignedRobot)
            assertEquals(nearRobot.id, assignedRobot!!.id)
            assertEquals(DeliveryStatus.ASSIGNED, delivery.status)
            assertEquals(RobotId(nearRobot.id), delivery.assignedRobotId)
        }

        @Test
        @DisplayName("사용 가능한 로봇이 없으면 null을 반환한다")
        fun `사용 가능한 로봇이 없으면 null을 반환한다`() {
            val delivery = createDelivery()

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertNull(assignedRobot)
            assertEquals(DeliveryStatus.PENDING, delivery.status)
            assertNull(delivery.assignedRobotId)
        }

        @Test
        @DisplayName("로봇에도 배달이 할당된다")
        fun `로봇에도 배달이 할당된다`() {
            val delivery = createDelivery()
            val robot =
                createRobot(
                    name = "로봇-1",
                    location = Location(latitude = 37.5665, longitude = 126.9780),
                )

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertNotNull(assignedRobot)
            assertEquals(RobotStatus.BUSY, assignedRobot!!.status)
            assertEquals(DeliveryId(delivery.id), assignedRobot.currentDeliveryId)
        }

        @Test
        @DisplayName("여러 로봇 중 가장 가까운 로봇이 선택된다")
        fun `여러 로봇 중 가장 가까운 로봇이 선택된다`() {
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val delivery = createDelivery(pickupLocation = pickupLocation)

            // 거리 순서: robot3 < robot1 < robot2
            val robot1 = createRobot(name = "로봇-1", location = Location(37.5010, 127.0010))
            val robot2 = createRobot(name = "로봇-2", location = Location(37.5020, 127.0020))
            val robot3 = createRobot(name = "로봇-3", location = Location(37.5005, 127.0005))

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertEquals(robot3.id, assignedRobot!!.id)
        }

        @Test
        @DisplayName("동일 거리의 로봇이 있으면 첫 번째 로봇이 선택된다")
        fun `동일 거리의 로봇이 있으면 첫 번째 로봇이 선택된다`() {
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val delivery = createDelivery(pickupLocation = pickupLocation)

            // 같은 위치의 로봇들
            val sameLocation = Location(latitude = 37.5010, longitude = 127.0010)
            val robot1 = createRobot(name = "로봇-1", location = sameLocation)
            val robot2 = createRobot(name = "로봇-2", location = sameLocation)

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            // 첫 번째로 추가된 로봇이 선택됨
            assertEquals(robot1.id, assignedRobot!!.id)
        }
    }
}
