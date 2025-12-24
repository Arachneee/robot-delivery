package com.robotdelivery.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestAsyncConfig::class, TestEventListenerConfig::class)
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport
