package com.robotdelivery.config

import com.robotdelivery.application.query.RobotQueryService
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
        robotQueryService: RobotQueryService,
    ): DeliveryAssignmentService = DeliveryAssignmentService(robotRepository, deliveryRepository, robotQueryService)
}
