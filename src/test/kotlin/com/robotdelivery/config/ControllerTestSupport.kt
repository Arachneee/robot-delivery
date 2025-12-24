package com.robotdelivery.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.robotdelivery.view.admin.AdminDeliveryController
import com.robotdelivery.view.delivery.DeliveryController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(value = [DeliveryController::class, AdminDeliveryController::class])
@AutoConfigureRestDocs
abstract class ControllerTestSupport {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    protected val objectMapper: ObjectMapper = jacksonObjectMapper()
}
