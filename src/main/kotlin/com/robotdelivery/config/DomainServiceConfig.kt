package com.robotdelivery.config

import com.robotdelivery.domain.delivery.DeliveryAssignmentService
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.robot.RobotRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainServiceConfig {
    @Bean
    fun deliveryAssignmentService(
        robotRepository: RobotRepository,
        deliveryRepository: DeliveryRepository,
    ): DeliveryAssignmentService = DeliveryAssignmentService(robotRepository, deliveryRepository)
}
