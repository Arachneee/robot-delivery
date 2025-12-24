package com.robotdelivery.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.task.SyncTaskExecutor
import java.util.concurrent.Executor

@TestConfiguration
class TestAsyncConfig {
    @Bean
    @Primary
    fun taskExecutor(): Executor = SyncTaskExecutor()
}

