package com.robotdelivery.config

import com.robotdelivery.domain.delivery.DeliveryAssignmentService
import com.robotdelivery.domain.delivery.DeliveryFactory
import com.robotdelivery.domain.delivery.DeliveryRepository
import com.robotdelivery.domain.robot.RobotAvailabilityService
import com.robotdelivery.domain.robot.RobotIotStateRepository
import com.robotdelivery.domain.robot.RobotMapClient
import com.robotdelivery.domain.robot.RobotRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainServiceConfig {
    @Bean
    fun robotAvailabilityService(
        robotRepository: RobotRepository,
        iotStateRepository: RobotIotStateRepository,
    ): RobotAvailabilityService = RobotAvailabilityService(robotRepository, iotStateRepository)

    @Bean
    fun deliveryAssignmentService(
        deliveryRepository: DeliveryRepository,
        robotAvailabilityService: RobotAvailabilityService,
        robotMapClient: RobotMapClient,
    ): DeliveryAssignmentService =
        DeliveryAssignmentService(deliveryRepository, robotAvailabilityService, robotMapClient)

    @Bean
    fun deliveryFactory(robotMapClient: RobotMapClient): DeliveryFactory = DeliveryFactory(robotMapClient)
}
