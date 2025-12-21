package com.robotdelivery

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class RobotDeliveryApplication

fun main(args: Array<String>) {
    runApplication<RobotDeliveryApplication>(*args)
}
