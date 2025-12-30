package com.robotdelivery.application

import com.robotdelivery.application.client.RobotClient
import com.robotdelivery.application.command.DeliveryService
import com.robotdelivery.application.command.vo.CreateDeliveryCommand
import com.robotdelivery.application.command.vo.DestinationInfo
import com.robotdelivery.config.IntegrationTestSupport
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.delivery.Delivery
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.delivery.DeliveryStatus
import com.robotdelivery.domain.delivery.Destination
import com.robotdelivery.domain.robot.Robot
import com.robotdelivery.domain.robot.RobotRepository
import com.robotdelivery.domain.robot.RobotStatus
import com.robotdelivery.domain.robot.findById
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean

@DisplayName("DeliveryService 테스트")
class DeliveryServiceTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var deliveryRepository: DeliveryRepository

    @Autowired
    private lateinit var robotRepository: RobotRepository

    @MockitoBean
    private lateinit var robotClient: RobotClient

    @Autowired
    private lateinit var deliveryService: DeliveryService

    @BeforeEach
    fun setUp() {
        deliveryRepository.deleteAll()
        robotRepository.deleteAll()
    }

    @Nested
    @DisplayName("배달 생성 테스트")
    inner class CreateDeliveryTest {
        @Test
        @DisplayName("배달을 성공적으로 생성한다")
        fun `배달을 성공적으로 생성한다`() {
            val command = createDeliveryCommand()

            val deliveryId = deliveryService.createDelivery(command)

            assertThat(deliveryId).isNotNull()
            val savedDelivery = deliveryRepository.findById(deliveryId)
            assertThat(savedDelivery).isNotNull()
        }

        @Test
        @DisplayName("생성된 배달은 PENDING 상태이다")
        fun `생성된 배달은 PENDING 상태이다`() {
            val command = createDeliveryCommand()

            val deliveryId = deliveryService.createDelivery(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertThat(savedDelivery.status).isEqualTo(DeliveryStatus.PENDING)
        }

        @Test
        @DisplayName("픽업 목적지 정보가 올바르게 저장된다")
        fun `픽업 목적지 정보가 올바르게 저장된다`() {
            val command = createDeliveryCommand()

            val deliveryId = deliveryService.createDelivery(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertThat(savedDelivery.pickupDestination.address).isEqualTo("서울시 중구 세종대로 110")
            assertThat(savedDelivery.pickupDestination.addressDetail).isEqualTo("시청역 1번 출구")
            assertThat(savedDelivery.pickupDestination.location.latitude).isEqualTo(37.5665)
            assertThat(savedDelivery.pickupDestination.location.longitude).isEqualTo(126.9780)
        }

        @Test
        @DisplayName("배송 목적지 정보가 올바르게 저장된다")
        fun `배송 목적지 정보가 올바르게 저장된다`() {
            val command = createDeliveryCommand()

            val deliveryId = deliveryService.createDelivery(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertThat(savedDelivery.deliveryDestination.address).isEqualTo("서울시 강남구 테헤란로 1")
            assertThat(savedDelivery.deliveryDestination.addressDetail).isEqualTo("2층")
            assertThat(savedDelivery.deliveryDestination.location.latitude).isEqualTo(37.4979)
            assertThat(savedDelivery.deliveryDestination.location.longitude).isEqualTo(127.0276)
        }

        @Test
        @DisplayName("전화번호가 올바르게 저장된다")
        fun `전화번호가 올바르게 저장된다`() {
            val command = createDeliveryCommand(phoneNumber = "010-9999-8888")

            val deliveryId = deliveryService.createDelivery(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertThat(savedDelivery.phoneNumber).isEqualTo("010-9999-8888")
        }

        @Test
        @DisplayName("주소 상세가 없어도 배달을 생성할 수 있다")
        fun `주소 상세가 없어도 배달을 생성할 수 있다`() {
            val command =
                createDeliveryCommand(
                    pickupAddressDetail = null,
                    deliveryAddressDetail = null,
                )

            val deliveryId = deliveryService.createDelivery(command)

            val savedDelivery = deliveryRepository.findById(deliveryId)!!
            assertThat(savedDelivery.pickupDestination.addressDetail).isNull()
            assertThat(savedDelivery.deliveryDestination.addressDetail).isNull()
        }
    }

    @Nested
    @DisplayName("배달 완료 테스트")
    inner class CompleteDeliveryTest {
        @Test
        @DisplayName("배달을 성공적으로 완료한다")
        fun `배달을 성공적으로 완료한다`() {
            val delivery = saveDeliveryInDroppingOffState()
            getAssignedRobot(delivery)

            deliveryService.completeDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.COMPLETED)
        }

        @Test
        @DisplayName("배달 완료 시 로봇이 READY 상태가 된다")
        fun `배달 완료 시 로봇이 READY 상태가 된다`() {
            val delivery = saveDeliveryInDroppingOffState()
            val robot = getAssignedRobot(delivery)

            deliveryService.completeDelivery(delivery.getDeliveryId())

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
            assertThat(updatedRobot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 완료 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 완료 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)

            assertThatThrownBy { deliveryService.completeDelivery(deliveryId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("배달을 찾을 수 없습니다: 99999")
        }

        @Test
        @DisplayName("배차되지 않은 배달 완료 시 예외가 발생한다")
        fun `배차되지 않은 배달 완료 시 예외가 발생한다`() {
            val delivery = saveDelivery()

            assertThatThrownBy { deliveryService.completeDelivery(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("배차된 로봇이 없습니다.")
        }
    }

    @Nested
    @DisplayName("문열기 테스트")
    inner class OpenDoorTest {
        @Test
        @DisplayName("픽업 도착 상태에서 문을 열면 PICKING_UP 상태가 된다")
        fun `픽업 도착 상태에서 문을 열면 PICKING_UP 상태가 된다`() {
            val delivery = saveDeliveryInPickupArrivedState()

            deliveryService.openDoor(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.PICKING_UP)
            verify(robotClient).openDoor(delivery.assignedRobotId!!)
        }

        @Test
        @DisplayName("배송 도착 상태에서 문을 열면 DROPPING_OFF 상태가 된다")
        fun `배송 도착 상태에서 문을 열면 DROPPING_OFF 상태가 된다`() {
            val delivery = saveDeliveryInDeliveryArrivedState()

            deliveryService.openDoor(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.DROPPING_OFF)
            verify(robotClient).openDoor(delivery.assignedRobotId!!)
        }

        @Test
        @DisplayName("문열기 시 RobotClient.openDoor가 호출된다")
        fun `문열기 시 RobotClient openDoor가 호출된다`() {
            val delivery = saveDeliveryInPickupArrivedState()

            deliveryService.openDoor(delivery.getDeliveryId())

            verify(robotClient).openDoor(delivery.assignedRobotId!!)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 문열기 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 문열기 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)

            assertThatThrownBy { deliveryService.openDoor(deliveryId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("배달을 찾을 수 없습니다: 99999")
        }

        @Test
        @DisplayName("배차되지 않은 배달의 문열기 시 예외가 발생한다")
        fun `배차되지 않은 배달의 문열기 시 예외가 발생한다`() {
            val delivery = saveDelivery()

            assertThatThrownBy { deliveryService.openDoor(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("배차된 로봇이 없습니다.")
        }

        @Test
        @DisplayName("문을 열 수 없는 상태에서 문열기 시 예외가 발생한다")
        fun `문을 열 수 없는 상태에서 문열기 시 예외가 발생한다`() {
            val delivery = saveDeliveryInAssignedState()

            assertThatThrownBy { deliveryService.openDoor(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("문을 열 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("배송 시작 테스트")
    inner class StartDeliveryTest {
        @Test
        @DisplayName("배송을 성공적으로 시작한다")
        fun `배송을 성공적으로 시작한다`() {
            val delivery = saveDeliveryInPickingUpState()
            getAssignedRobot(delivery)

            deliveryService.startDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.DELIVERING)
        }

        @Test
        @DisplayName("배송 시작 시 로봇에게 배송 목적지로 이동 명령이 전달된다")
        fun `배송 시작 시 로봇에게 배송 목적지로 이동 명령이 전달된다`() {
            val delivery = saveDeliveryInPickingUpState()
            val robot = getAssignedRobot(delivery)

            deliveryService.startDelivery(delivery.getDeliveryId())

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.destination).isEqualTo(delivery.deliveryDestination.location)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 배송 시작 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 배송 시작 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)

            assertThatThrownBy { deliveryService.startDelivery(deliveryId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("배달을 찾을 수 없습니다: 99999")
        }

        @Test
        @DisplayName("배차되지 않은 배달의 배송 시작 시 예외가 발생한다")
        fun `배차되지 않은 배달의 배송 시작 시 예외가 발생한다`() {
            val delivery = saveDelivery()

            assertThatThrownBy { deliveryService.startDelivery(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("배차된 로봇이 없습니다.")
        }

        @Test
        @DisplayName("픽업 중 상태가 아닌 배달의 배송 시작 시 예외가 발생한다")
        fun `픽업 중 상태가 아닌 배달의 배송 시작 시 예외가 발생한다`() {
            val delivery = saveDeliveryInAssignedState()
            getAssignedRobot(delivery)

            assertThatThrownBy { deliveryService.startDelivery(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("픽업 중 상태에서만 배송을 시작할 수 있습니다")
        }
    }

    @Nested
    @DisplayName("회수 완료 테스트")
    inner class CompleteReturnTest {
        @Test
        @DisplayName("회수를 성공적으로 완료한다")
        fun `회수를 성공적으로 완료한다`() {
            val delivery = saveDeliveryInReturningOffState()
            getAssignedRobot(delivery)

            deliveryService.completeReturn(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.RETURN_COMPLETED)
        }

        @Test
        @DisplayName("회수 완료 시 로봇이 READY 상태가 된다")
        fun `회수 완료 시 로봇이 READY 상태가 된다`() {
            val delivery = saveDeliveryInReturningOffState()
            val robot = getAssignedRobot(delivery)

            deliveryService.completeReturn(delivery.getDeliveryId())

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
            assertThat(updatedRobot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 회수 완료 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 회수 완료 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)

            assertThatThrownBy { deliveryService.completeReturn(deliveryId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("배달을 찾을 수 없습니다: 99999")
        }

        @Test
        @DisplayName("배차되지 않은 배달 회수 완료 시 예외가 발생한다")
        fun `배차되지 않은 배달 회수 완료 시 예외가 발생한다`() {
            val delivery = saveDelivery()

            assertThatThrownBy { deliveryService.completeReturn(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("배차된 로봇이 없습니다.")
        }

        @Test
        @DisplayName("회수 중 상태가 아닌 배달의 회수 완료 시 예외가 발생한다")
        fun `회수 중 상태가 아닌 배달의 회수 완료 시 예외가 발생한다`() {
            val delivery = saveDeliveryInAssignedState()
            getAssignedRobot(delivery)

            assertThatThrownBy { deliveryService.completeReturn(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("회수 완료 처리할 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("배달 취소 테스트")
    inner class CancelDeliveryTest {
        @Test
        @DisplayName("PENDING 상태의 배달을 취소한다")
        fun `PENDING 상태의 배달을 취소한다`() {
            val delivery = saveDelivery()

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.CANCELED)
            assertThat(requiresReturn).isFalse()
        }

        @Test
        @DisplayName("ASSIGNED 상태의 배달을 취소하면 로봇이 해제된다")
        fun `ASSIGNED 상태의 배달을 취소하면 로봇이 해제된다`() {
            val delivery = saveDeliveryInAssignedState()
            val robot = getAssignedRobot(delivery)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.CANCELED)
            assertThat(requiresReturn).isFalse()
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
            assertThat(updatedRobot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태의 배달을 취소하면 로봇이 해제된다")
        fun `PICKUP_ARRIVED 상태의 배달을 취소하면 로봇이 해제된다`() {
            val delivery = saveDeliveryInPickupArrivedState()
            val robot = getAssignedRobot(delivery)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.CANCELED)
            assertThat(requiresReturn).isFalse()
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
            assertThat(updatedRobot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("PICKING_UP 상태의 배달을 취소하면 회수가 시작된다")
        fun `PICKING_UP 상태의 배달을 취소하면 회수가 시작된다`() {
            val delivery = saveDeliveryInPickingUpState()
            val robot = getAssignedRobot(delivery)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.RETURNING)
            assertThat(requiresReturn).isTrue()
            assertThat(updatedRobot.destination).isEqualTo(delivery.pickupDestination.location)
        }

        @Test
        @DisplayName("DELIVERING 상태의 배달을 취소하면 회수가 시작된다")
        fun `DELIVERING 상태의 배달을 취소하면 회수가 시작된다`() {
            val delivery = saveDeliveryInDeliveringState()
            val robot = getAssignedRobot(delivery)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.RETURNING)
            assertThat(requiresReturn).isTrue()
            assertThat(updatedRobot.destination).isEqualTo(delivery.pickupDestination.location)
        }

        @Test
        @DisplayName("DROPPING_OFF 상태의 배달을 취소하면 회수가 시작된다")
        fun `DROPPING_OFF 상태의 배달을 취소하면 회수가 시작된다`() {
            val delivery = saveDeliveryInDroppingOffState()
            val robot = getAssignedRobot(delivery)

            val requiresReturn = deliveryService.cancelDelivery(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.RETURNING)
            assertThat(requiresReturn).isTrue()
            assertThat(updatedRobot.destination).isEqualTo(delivery.pickupDestination.location)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 취소 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 취소 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)

            assertThatThrownBy { deliveryService.cancelDelivery(deliveryId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("배달을 찾을 수 없습니다: 99999")
        }

        @Test
        @DisplayName("이미 완료된 배달은 취소할 수 없다")
        fun `이미 완료된 배달은 취소할 수 없다`() {
            val delivery = saveDeliveryInCompletedState()

            assertThatThrownBy { deliveryService.cancelDelivery(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("취소할 수 없는 상태입니다")
        }

        @Test
        @DisplayName("이미 취소된 배달은 다시 취소할 수 없다")
        fun `이미 취소된 배달은 다시 취소할 수 없다`() {
            val delivery = saveDeliveryInCanceledState()

            assertThatThrownBy { deliveryService.cancelDelivery(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("취소할 수 없는 상태입니다")
        }
    }

    @Nested
    @DisplayName("배차 취소 테스트")
    inner class UnassignRobotTest {
        @Test
        @DisplayName("배차를 성공적으로 취소한다")
        fun `배차를 성공적으로 취소한다`() {
            val delivery = saveDeliveryInAssignedState()
            getAssignedRobot(delivery)

            deliveryService.unassignRobot(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(updatedDelivery.assignedRobotId).isNull()
        }

        @Test
        @DisplayName("배차 취소 시 로봇이 READY 상태가 된다")
        fun `배차 취소 시 로봇이 READY 상태가 된다`() {
            val delivery = saveDeliveryInAssignedState()
            val robot = getAssignedRobot(delivery)

            deliveryService.unassignRobot(delivery.getDeliveryId())

            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
            assertThat(updatedRobot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("PICKUP_ARRIVED 상태에서 배차를 취소할 수 있다")
        fun `PICKUP_ARRIVED 상태에서 배차를 취소할 수 있다`() {
            val delivery = saveDeliveryInPickupArrivedState()
            val robot = getAssignedRobot(delivery)

            deliveryService.unassignRobot(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
        }

        @Test
        @DisplayName("PICKING_UP 상태에서 배차를 취소할 수 있다")
        fun `PICKING_UP 상태에서 배차를 취소할 수 있다`() {
            val delivery = saveDeliveryInPickingUpState()
            val robot = getAssignedRobot(delivery)

            deliveryService.unassignRobot(delivery.getDeliveryId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.PENDING)
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 배차 취소 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 배차 취소 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)

            assertThatThrownBy { deliveryService.unassignRobot(deliveryId) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("배달을 찾을 수 없습니다: 99999")
        }

        @Test
        @DisplayName("배차되지 않은 배달의 배차 취소 시 예외가 발생한다")
        fun `배차되지 않은 배달의 배차 취소 시 예외가 발생한다`() {
            val delivery = saveDelivery()

            assertThatThrownBy { deliveryService.unassignRobot(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("배차된 로봇이 없습니다.")
        }

        @Test
        @DisplayName("배달 출발 후에는 배차 취소할 수 없다")
        fun `배달 출발 후에는 배차 취소할 수 없다`() {
            val delivery = saveDeliveryInDeliveringState()
            getAssignedRobot(delivery)

            assertThatThrownBy { deliveryService.unassignRobot(delivery.getDeliveryId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("배달 출발 전 상태에서만 배차 취소가 가능합니다")
        }
    }

    @Nested
    @DisplayName("배차 변경 테스트")
    inner class ReassignRobotTest {
        @Test
        @DisplayName("배차를 성공적으로 변경한다")
        fun `배차를 성공적으로 변경한다`() {
            val delivery = saveDeliveryInAssignedState()
            val previousRobot = getAssignedRobot(delivery)
            val newRobot = saveReadyRobot(name = "로봇-2")

            val previousRobotId = deliveryService.reassignRobot(delivery.getDeliveryId(), newRobot.getRobotId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(previousRobotId).isEqualTo(previousRobot.getRobotId())
            assertThat(updatedDelivery.assignedRobotId).isEqualTo(newRobot.getRobotId())
        }

        @Test
        @DisplayName("배차 변경 시 이전 로봇은 READY 상태가 된다")
        fun `배차 변경 시 이전 로봇은 READY 상태가 된다`() {
            val delivery = saveDeliveryInAssignedState()
            val previousRobot = getAssignedRobot(delivery)
            val newRobot = saveReadyRobot(name = "로봇-2")

            deliveryService.reassignRobot(delivery.getDeliveryId(), newRobot.getRobotId())

            val updatedPreviousRobot = robotRepository.findById(previousRobot.getRobotId())!!
            assertThat(updatedPreviousRobot.status).isEqualTo(RobotStatus.READY)
            assertThat(updatedPreviousRobot.currentDeliveryId).isNull()
        }

        @Test
        @DisplayName("배차 변경 시 새 로봇은 BUSY 상태가 된다")
        fun `배차 변경 시 새 로봇은 BUSY 상태가 된다`() {
            val delivery = saveDeliveryInAssignedState()
            getAssignedRobot(delivery)
            val newRobot = saveReadyRobot(name = "로봇-2")

            deliveryService.reassignRobot(delivery.getDeliveryId(), newRobot.getRobotId())

            val updatedNewRobot = robotRepository.findById(newRobot.getRobotId())!!
            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(updatedNewRobot.status).isEqualTo(RobotStatus.BUSY)
            assertThat(updatedNewRobot.currentDeliveryId).isEqualTo(updatedDelivery.getDeliveryId())
        }

        @Test
        @DisplayName("로봇이 없는 배달에 신규 배차하면 null을 반환한다")
        fun `로봇이 없는 배달에 신규 배차하면 null을 반환한다`() {
            val delivery = saveDelivery()
            val newRobot = saveReadyRobot(name = "로봇-2")

            val previousRobotId = deliveryService.reassignRobot(delivery.getDeliveryId(), newRobot.getRobotId())

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            assertThat(previousRobotId).isNull()
            assertThat(updatedDelivery.assignedRobotId).isEqualTo(newRobot.getRobotId())
            assertThat(updatedDelivery.status).isEqualTo(DeliveryStatus.ASSIGNED)
        }

        @Test
        @DisplayName("새 로봇이 사용 불가 상태면 예외가 발생한다")
        fun `새 로봇이 사용 불가 상태면 예외가 발생한다`() {
            val delivery = saveDeliveryInAssignedState()
            getAssignedRobot(delivery)
            val newRobot = saveOffDutyRobot(name = "로봇-2")

            assertThatThrownBy { deliveryService.reassignRobot(delivery.getDeliveryId(), newRobot.getRobotId()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("새 로봇이 배차 가능한 상태가 아닙니다")
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 배차 변경 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 배차 변경 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)
            val newRobot = saveReadyRobot(name = "로봇-2")

            assertThatThrownBy { deliveryService.reassignRobot(deliveryId, newRobot.getRobotId()) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("배달을 찾을 수 없습니다: 99999")
        }
    }

    @Nested
    @DisplayName("상태 변경 테스트")
    inner class ChangeStatusTest {
        @Test
        @DisplayName("PENDING에서 CANCELED로 변경한다")
        fun `PENDING에서 CANCELED로 변경한다`() {
            val delivery = saveDelivery()

            val result = deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.CANCELED)

            assertThat(result.previousStatus).isEqualTo(DeliveryStatus.PENDING)
            assertThat(result.currentStatus).isEqualTo(DeliveryStatus.CANCELED)
        }

        @Test
        @DisplayName("ASSIGNED에서 PICKUP_ARRIVED로 변경한다")
        fun `ASSIGNED에서 PICKUP_ARRIVED로 변경한다`() {
            val delivery = saveDeliveryInAssignedState()

            val result = deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.PICKUP_ARRIVED)

            assertThat(result.previousStatus).isEqualTo(DeliveryStatus.ASSIGNED)
            assertThat(result.currentStatus).isEqualTo(DeliveryStatus.PICKUP_ARRIVED)
        }

        @Test
        @DisplayName("PICKUP_ARRIVED에서 PICKING_UP으로 변경한다")
        fun `PICKUP_ARRIVED에서 PICKING_UP으로 변경한다`() {
            val delivery = saveDeliveryInPickupArrivedState()

            val result = deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.PICKING_UP)

            assertThat(result.previousStatus).isEqualTo(DeliveryStatus.PICKUP_ARRIVED)
            assertThat(result.currentStatus).isEqualTo(DeliveryStatus.PICKING_UP)
        }

        @Test
        @DisplayName("PICKING_UP에서 DELIVERING으로 변경한다")
        fun `PICKING_UP에서 DELIVERING으로 변경한다`() {
            val delivery = saveDeliveryInPickingUpState()
            getAssignedRobot(delivery)

            val result = deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.DELIVERING)

            assertThat(result.previousStatus).isEqualTo(DeliveryStatus.PICKING_UP)
            assertThat(result.currentStatus).isEqualTo(DeliveryStatus.DELIVERING)
        }

        @Test
        @DisplayName("DROPPING_OFF에서 COMPLETED로 변경한다")
        fun `DROPPING_OFF에서 COMPLETED로 변경한다`() {
            val delivery = saveDeliveryInDroppingOffState()
            getAssignedRobot(delivery)

            val result = deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.COMPLETED)

            assertThat(result.previousStatus).isEqualTo(DeliveryStatus.DROPPING_OFF)
            assertThat(result.currentStatus).isEqualTo(DeliveryStatus.COMPLETED)
        }

        @Test
        @DisplayName("DELIVERING에서 RETURNING으로 변경한다")
        fun `DELIVERING에서 RETURNING으로 변경한다`() {
            val delivery = saveDeliveryInDeliveringState()
            getAssignedRobot(delivery)

            val result = deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.RETURNING)

            assertThat(result.previousStatus).isEqualTo(DeliveryStatus.DELIVERING)
            assertThat(result.currentStatus).isEqualTo(DeliveryStatus.RETURNING)
        }

        @Test
        @DisplayName("ASSIGNED로 변경하려고 하면 예외가 발생한다")
        fun `ASSIGNED로 변경하려고 하면 예외가 발생한다`() {
            val delivery = saveDelivery()

            assertThatThrownBy { deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.ASSIGNED) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("ASSIGNED 상태로의 변경은 배차 API를 사용해주세요")
        }

        @Test
        @DisplayName("전이할 수 없는 상태로 변경하면 예외가 발생한다")
        fun `전이할 수 없는 상태로 변경하면 예외가 발생한다`() {
            val delivery = saveDelivery()

            assertThatThrownBy { deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.COMPLETED) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("PENDING")
                .hasMessageContaining("COMPLETED")
        }

        @Test
        @DisplayName("존재하지 않는 배달 ID로 상태 변경 시 예외가 발생한다")
        fun `존재하지 않는 배달 ID로 상태 변경 시 예외가 발생한다`() {
            val deliveryId = DeliveryId(99999L)

            assertThatThrownBy { deliveryService.changeStatus(deliveryId, DeliveryStatus.CANCELED) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("배달을 찾을 수 없습니다: 99999")
        }

        @Test
        @DisplayName("ASSIGNED에서 PENDING으로 변경하면 배차가 취소된다")
        fun `ASSIGNED에서 PENDING으로 변경하면 배차가 취소된다`() {
            val delivery = saveDeliveryInAssignedState()
            val robot = getAssignedRobot(delivery)

            val result = deliveryService.changeStatus(delivery.getDeliveryId(), DeliveryStatus.PENDING)

            val updatedDelivery = deliveryRepository.findById(delivery.getDeliveryId())!!
            val updatedRobot = robotRepository.findById(robot.getRobotId())!!
            assertThat(result.previousStatus).isEqualTo(DeliveryStatus.ASSIGNED)
            assertThat(result.currentStatus).isEqualTo(DeliveryStatus.PENDING)
            assertThat(updatedDelivery.assignedRobotId).isNull()
            assertThat(updatedRobot.status).isEqualTo(RobotStatus.READY)
        }
    }

    private fun createDeliveryCommand(
        pickupAddress: String = "서울시 중구 세종대로 110",
        pickupAddressDetail: String? = "시청역 1번 출구",
        pickupLatitude: Double = 37.5665,
        pickupLongitude: Double = 126.9780,
        deliveryAddress: String = "서울시 강남구 테헤란로 1",
        deliveryAddressDetail: String? = "2층",
        deliveryLatitude: Double = 37.4979,
        deliveryLongitude: Double = 127.0276,
        phoneNumber: String = "010-1234-5678",
    ): CreateDeliveryCommand =
        CreateDeliveryCommand(
            pickupDestination =
                DestinationInfo(
                    address = pickupAddress,
                    addressDetail = pickupAddressDetail,
                    latitude = pickupLatitude,
                    longitude = pickupLongitude,
                ),
            deliveryDestination =
                DestinationInfo(
                    address = deliveryAddress,
                    addressDetail = deliveryAddressDetail,
                    latitude = deliveryLatitude,
                    longitude = deliveryLongitude,
                ),
            phoneNumber = phoneNumber,
        )

    private fun saveDelivery(): Delivery {
        val delivery =
            Delivery(
                pickupDestination =
                    Destination(
                        address = "서울시 중구 세종대로 110",
                        location = Location(latitude = 37.5665, longitude = 126.9780),
                    ),
                deliveryDestination =
                    Destination(
                        address = "서울시 강남구 테헤란로 1",
                        location = Location(latitude = 37.4979, longitude = 127.0276),
                    ),
                phoneNumber = "010-1234-5678",
            )
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInAssignedState(): Delivery {
        val robot = saveRobotForDelivery()
        val delivery = saveDelivery()
        delivery.assignRobot(robot.getRobotId())
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInPickupArrivedState(): Delivery {
        val delivery = saveDeliveryInAssignedState()
        delivery.arrived()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInDeliveryArrivedState(): Delivery {
        val delivery = saveDeliveryInPickupArrivedState()
        delivery.openDoor()
        delivery.startDelivery()
        delivery.arrived()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInPickingUpState(): Delivery {
        val delivery = saveDeliveryInPickupArrivedState()
        delivery.openDoor()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInDroppingOffState(): Delivery {
        val delivery = saveDeliveryInDeliveryArrivedState()
        delivery.openDoor()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInReturningOffState(): Delivery {
        val delivery = saveDeliveryInDroppingOffState()
        delivery.cancel()
        delivery.arrived()
        delivery.openDoor()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInDeliveringState(): Delivery {
        val delivery = saveDeliveryInPickingUpState()
        delivery.startDelivery()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInCompletedState(): Delivery {
        val delivery = saveDeliveryInDroppingOffState()
        delivery.complete()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveDeliveryInCanceledState(): Delivery {
        val delivery = saveDelivery()
        delivery.cancel()
        delivery.pullDomainEvents()
        return deliveryRepository.saveAndFlush(delivery)
    }

    private fun saveRobot(name: String = "로봇-1"): Robot {
        val robot = Robot(name = name, status = RobotStatus.OFF_DUTY)
        return robotRepository.saveAndFlush(robot)
    }

    private fun saveRobotForDelivery(name: String = "로봇-1"): Robot {
        val robot = saveRobot(name)
        robot.startDuty()
        robot.pullDomainEvents()
        return robotRepository.saveAndFlush(robot)
    }

    private fun getAssignedRobot(delivery: Delivery): Robot {
        val robotId = delivery.assignedRobotId ?: throw IllegalStateException("배달에 로봇이 할당되어 있지 않습니다.")
        val robot = robotRepository.findById(robotId) ?: throw IllegalStateException("로봇을 찾을 수 없습니다: $robotId")
        robot.assignDelivery(delivery.getDeliveryId(), delivery.pickupDestination.location)
        robot.pullDomainEvents()
        return robotRepository.saveAndFlush(robot)
    }

    private fun saveReadyRobot(name: String = "로봇-1"): Robot {
        val robot = saveRobot(name)
        robot.startDuty()
        robot.pullDomainEvents()
        return robotRepository.saveAndFlush(robot)
    }

    private fun saveOffDutyRobot(name: String = "로봇-1"): Robot = saveRobot(name)
}
