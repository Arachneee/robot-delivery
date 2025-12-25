package com.robotdelivery.domain.delivery

import com.robotdelivery.config.IntegrationTestSupport
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotIotState
import com.robotdelivery.domain.robot.RobotIotStateRepository
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.infrastructure.persistence.InMemoryRobotIotStateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("DeliveryAssignmentService 테스트")
class DeliveryAssignmentServiceTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var robotRepository: RobotRepository

    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var iotStateRepository: RobotIotStateRepository

    @Autowired
    private lateinit var assignmentService: DeliveryAssignmentService

    @BeforeEach
    fun setUp() {
        deliveryRepository.deleteAll()
        robotRepository.deleteAll()
        (iotStateRepository as InMemoryRobotIotStateRepository).deleteAll()
    }

    private fun saveDelivery(pickupLocation: Location = Location(latitude = 37.5665, longitude = 126.9780)): Delivery {
        val delivery =
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
            )
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveReadyRobot(
        name: String,
        location: Location,
        battery: Int = 100,
    ): Robot {
        val robot = Robot(name = name, status = RobotStatus.OFF_DUTY)
        val savedRobot = robotRepository.saveAndFlush(robot)
        savedRobot.startDuty()
        savedRobot.pullDomainEvents()
        val readyRobot = robotRepository.saveAndFlush(savedRobot)

        iotStateRepository.save(
            RobotIotState(
                robotId = readyRobot.getRobotId(),
                location = location,
                battery = battery,
                doorOpen = false,
                loadWeight = 0.0,
            ),
        )

        return readyRobot
    }

    @Nested
    @DisplayName("가장 가까운 로봇 배정 테스트")
    inner class AssignNearestRobotTest {
        @Test
        @DisplayName("가장 가까운 로봇이 배정된다")
        fun `가장 가까운 로봇이 배정된다`() {
            val pickupLocation = Location(latitude = 37.5665, longitude = 126.9780)
            val delivery = saveDelivery(pickupLocation = pickupLocation)

            val farRobot =
                saveReadyRobot(
                    name = "로봇-far",
                    location = Location(latitude = 37.4979, longitude = 127.0276),
                )
            val nearRobot =
                saveReadyRobot(
                    name = "로봇-near",
                    location = Location(latitude = 37.5660, longitude = 126.9770),
                )

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertThat(assignedRobot).isNotNull
            assertThat(assignedRobot!!.id).isEqualTo(nearRobot.id)
            assertThat(delivery.status).isEqualTo(DeliveryStatus.ASSIGNED)
            assertThat(delivery.assignedRobotId).isEqualTo(nearRobot.getRobotId())
        }

        @Test
        @DisplayName("사용 가능한 로봇이 없으면 null을 반환한다")
        fun `사용 가능한 로봇이 없으면 null을 반환한다`() {
            val delivery = saveDelivery()

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertThat(assignedRobot).isNull()
            assertThat(delivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(delivery.assignedRobotId).isNull()
        }

        @Test
        @DisplayName("로봇에도 배달이 할당된다")
        fun `로봇에도 배달이 할당된다`() {
            val delivery = saveDelivery()
            saveReadyRobot(
                name = "로봇-1",
                location = Location(latitude = 37.5665, longitude = 126.9780),
            )

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertThat(assignedRobot).isNotNull
            assertThat(assignedRobot!!.status).isEqualTo(RobotStatus.BUSY)
            assertThat(assignedRobot.currentDeliveryId).isEqualTo(delivery.getDeliveryId())
        }

        @Test
        @DisplayName("여러 로봇 중 가장 가까운 로봇이 선택된다")
        fun `여러 로봇 중 가장 가까운 로봇이 선택된다`() {
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val delivery = saveDelivery(pickupLocation = pickupLocation)

            saveReadyRobot(name = "로봇-1", location = Location(37.5010, 127.0010))
            saveReadyRobot(name = "로봇-2", location = Location(37.5020, 127.0020))
            val robot3 = saveReadyRobot(name = "로봇-3", location = Location(37.5005, 127.0005))

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertThat(assignedRobot!!.id).isEqualTo(robot3.id)
        }

        @Test
        @DisplayName("동일 거리의 로봇이 있으면 첫 번째 로봇이 선택된다")
        fun `동일 거리의 로봇이 있으면 첫 번째 로봇이 선택된다`() {
            val pickupLocation = Location(latitude = 37.5000, longitude = 127.0000)
            val delivery = saveDelivery(pickupLocation = pickupLocation)

            val sameLocation = Location(latitude = 37.5010, longitude = 127.0010)
            val robot1 = saveReadyRobot(name = "로봇-1", location = sameLocation)
            saveReadyRobot(name = "로봇-2", location = sameLocation)

            val assignedRobot = assignmentService.assignNearestRobotToDelivery(delivery)

            assertThat(assignedRobot!!.id).isEqualTo(robot1.id)
        }
    }
}
