package com.robotdelivery.domain.delivery.event

import com.robotdelivery.domain.common.vo.DeliveryId
import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.delivery.vo.DestinationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

@DisplayName("DeliveryEventHistory 테스트")
class DeliveryEventHistoryTest {
    @Test
    @DisplayName("DeliveryCreatedEvent로부터 이력을 생성한다")
    fun `DeliveryCreatedEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryCreatedEvent(
                deliveryId = DeliveryId(1L),
                pickupLocation = Location(37.5665, 126.9780),
                deliveryLocation = Location(37.4979, 127.0276),
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(1L)
        assertThat(history.eventType).isEqualTo("DeliveryCreatedEvent")
        assertThat(history.robotId).isNull()
        assertThat(history.occurredAt).isEqualTo(event.occurredAt)
    }

    @Test
    @DisplayName("DeliveryRobotAssignedEvent로부터 로봇 ID를 포함한 이력을 생성한다")
    fun `DeliveryRobotAssignedEvent로부터 로봇 ID를 포함한 이력을 생성한다`() {
        val event =
            DeliveryRobotAssignedEvent(
                deliveryId = DeliveryId(2L),
                robotId = RobotId(5L),
                pickupLocation = Location(37.5665, 126.9780),
                estimatedPickupDuration = Duration.ofSeconds(300),
                estimatedDeliveryDuration = Duration.ofSeconds(600),
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(2L)
        assertThat(history.eventType).isEqualTo("DeliveryRobotAssignedEvent")
        assertThat(history.robotId).isEqualTo(5L)
        assertThat(history.occurredAt).isEqualTo(event.occurredAt)
    }

    @Test
    @DisplayName("DeliveryStartedEvent로부터 이력을 생성한다")
    fun `DeliveryStartedEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryStartedEvent(
                deliveryId = DeliveryId(3L),
                robotId = RobotId(7L),
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(3L)
        assertThat(history.eventType).isEqualTo("DeliveryStartedEvent")
        assertThat(history.robotId).isEqualTo(7L)
    }

    @Test
    @DisplayName("DeliveryCompletedEvent로부터 이력을 생성한다")
    fun `DeliveryCompletedEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryCompletedEvent(
                deliveryId = DeliveryId(4L),
                robotId = RobotId(8L),
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(4L)
        assertThat(history.eventType).isEqualTo("DeliveryCompletedEvent")
        assertThat(history.robotId).isEqualTo(8L)
    }

    @Test
    @DisplayName("DeliveryCanceledEvent에 로봇 ID가 있으면 포함하여 이력을 생성한다")
    fun `DeliveryCanceledEvent에 로봇 ID가 있으면 포함하여 이력을 생성한다`() {
        val event =
            DeliveryCanceledEvent(
                deliveryId = DeliveryId(5L),
                robotId = RobotId(9L),
                requiresReturn = true,
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(5L)
        assertThat(history.eventType).isEqualTo("DeliveryCanceledEvent")
        assertThat(history.robotId).isEqualTo(9L)
    }

    @Test
    @DisplayName("DeliveryCanceledEvent에 로봇 ID가 없으면 null로 이력을 생성한다")
    fun `DeliveryCanceledEvent에 로봇 ID가 없으면 null로 이력을 생성한다`() {
        val event =
            DeliveryCanceledEvent(
                deliveryId = DeliveryId(6L),
                robotId = null,
                requiresReturn = false,
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(6L)
        assertThat(history.eventType).isEqualTo("DeliveryCanceledEvent")
        assertThat(history.robotId).isNull()
    }

    @Test
    @DisplayName("DeliveryApproachingEvent로부터 이력을 생성한다")
    fun `DeliveryApproachingEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryApproachingEvent(
                deliveryId = DeliveryId(7L),
                robotId = RobotId(10L),
                destination = Location(37.5665, 126.9780),
                destinationType = DestinationType.PICKUP,
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(7L)
        assertThat(history.eventType).isEqualTo("DeliveryApproachingEvent")
        assertThat(history.robotId).isEqualTo(10L)
    }

    @Test
    @DisplayName("DeliveryArrivedEvent로부터 이력을 생성한다")
    fun `DeliveryArrivedEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryArrivedEvent(
                deliveryId = DeliveryId(8L),
                robotId = RobotId(11L),
                destination = Location(37.4979, 127.0276),
                destinationType = DestinationType.DELIVERY,
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(8L)
        assertThat(history.eventType).isEqualTo("DeliveryArrivedEvent")
        assertThat(history.robotId).isEqualTo(11L)
    }

    @Test
    @DisplayName("DeliveryReturnStartedEvent로부터 이력을 생성한다")
    fun `DeliveryReturnStartedEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryReturnStartedEvent(
                deliveryId = DeliveryId(9L),
                robotId = RobotId(12L),
                returnLocation = Location(37.5665, 126.9780),
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(9L)
        assertThat(history.eventType).isEqualTo("DeliveryReturnStartedEvent")
        assertThat(history.robotId).isEqualTo(12L)
    }

    @Test
    @DisplayName("DeliveryReturnCompletedEvent로부터 이력을 생성한다")
    fun `DeliveryReturnCompletedEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryReturnCompletedEvent(
                deliveryId = DeliveryId(10L),
                robotId = RobotId(13L),
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(10L)
        assertThat(history.eventType).isEqualTo("DeliveryReturnCompletedEvent")
        assertThat(history.robotId).isEqualTo(13L)
    }

    @Test
    @DisplayName("DeliveryRobotUnassignedEvent로부터 이력을 생성한다")
    fun `DeliveryRobotUnassignedEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryRobotUnassignedEvent(
                deliveryId = DeliveryId(11L),
                robotId = RobotId(14L),
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(11L)
        assertThat(history.eventType).isEqualTo("DeliveryRobotUnassignedEvent")
        assertThat(history.robotId).isEqualTo(14L)
    }

    @Test
    @DisplayName("DeliveryRobotReassignedEvent로부터 이력을 생성한다")
    fun `DeliveryRobotReassignedEvent로부터 이력을 생성한다`() {
        val event =
            DeliveryRobotReassignedEvent(
                deliveryId = DeliveryId(12L),
                previousRobotId = RobotId(15L),
                newRobotId = RobotId(16L),
                estimatedPickupDuration = java.time.Duration.ofMinutes(5),
                estimatedDeliveryDuration = java.time.Duration.ofMinutes(10),
            )

        val history = DeliveryEventHistory.from(event)

        assertThat(history.deliveryId).isEqualTo(12L)
        assertThat(history.eventType).isEqualTo("DeliveryRobotReassignedEvent")
        assertThat(history.robotId).isEqualTo(16L)
    }
}
