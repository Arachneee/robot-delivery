package com.robotdelivery.presentation.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.robotdelivery.application.DeliveryService
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.presentation.delivery.dto.CreateDeliveryRequest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.given
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(DeliveryController::class)
@AutoConfigureRestDocs
@DisplayName("DeliveryController 테스트")
class DeliveryControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @MockitoBean
    private lateinit var deliveryService: DeliveryService

    @Test
    @DisplayName("배달 생성 API - 성공")
    fun `배달 생성 API 성공`() {
        // given
        val request =
            CreateDeliveryRequest(
                pickupAddress = "서울시 중구 세종대로 110",
                pickupAddressDetail = "시청역 1번 출구",
                pickupLatitude = 37.5665,
                pickupLongitude = 126.9780,
                deliveryAddress = "서울시 강남구 테헤란로 521",
                deliveryAddressDetail = "삼성역 2번 출구",
                deliveryLatitude = 37.5087,
                deliveryLongitude = 127.0632,
                phoneNumber = "010-1234-5678",
            )

        val deliveryId = DeliveryId(1L)
        given(
            deliveryService.createDelivery(
                pickupAddress = any(),
                pickupAddressDetail = anyOrNull(),
                pickupLatitude = any(),
                pickupLongitude = any(),
                deliveryAddress = any(),
                deliveryAddressDetail = anyOrNull(),
                deliveryLatitude = any(),
                deliveryLongitude = any(),
                phoneNumber = any(),
            ),
        ).willReturn(deliveryId)

        // when & then
        mockMvc
            .perform(
                post("/api/deliveries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/deliveries/1"))
            .andExpect(jsonPath("$.deliveryId").value(1))
            .andExpect(jsonPath("$.message").value("배달이 성공적으로 생성되었습니다."))
            .andDo(
                document(
                    "delivery-create",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("pickupAddress")
                            .type(JsonFieldType.STRING)
                            .description("픽업 주소"),
                        fieldWithPath("pickupAddressDetail")
                            .type(JsonFieldType.STRING)
                            .description("픽업 상세 주소")
                            .optional(),
                        fieldWithPath("pickupLatitude")
                            .type(JsonFieldType.NUMBER)
                            .description("픽업 위치 위도"),
                        fieldWithPath("pickupLongitude")
                            .type(JsonFieldType.NUMBER)
                            .description("픽업 위치 경도"),
                        fieldWithPath("deliveryAddress")
                            .type(JsonFieldType.STRING)
                            .description("배달 주소"),
                        fieldWithPath("deliveryAddressDetail")
                            .type(JsonFieldType.STRING)
                            .description("배달 상세 주소")
                            .optional(),
                        fieldWithPath("deliveryLatitude")
                            .type(JsonFieldType.NUMBER)
                            .description("배달 위치 위도"),
                        fieldWithPath("deliveryLongitude")
                            .type(JsonFieldType.NUMBER)
                            .description("배달 위치 경도"),
                        fieldWithPath("phoneNumber")
                            .type(JsonFieldType.STRING)
                            .description("연락처"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("생성된 배달 ID"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("배달 생성 API - 상세 주소 없이 생성")
    fun `배달 생성 API 상세 주소 없이 성공`() {
        // given
        val request =
            CreateDeliveryRequest(
                pickupAddress = "서울시 중구 세종대로 110",
                pickupAddressDetail = null,
                pickupLatitude = 37.5665,
                pickupLongitude = 126.9780,
                deliveryAddress = "서울시 강남구 테헤란로 521",
                deliveryAddressDetail = null,
                deliveryLatitude = 37.5087,
                deliveryLongitude = 127.0632,
                phoneNumber = "010-1234-5678",
            )

        val deliveryId = DeliveryId(2L)
        given(
            deliveryService.createDelivery(
                pickupAddress = any(),
                pickupAddressDetail = anyOrNull(),
                pickupLatitude = any(),
                pickupLongitude = any(),
                deliveryAddress = any(),
                deliveryAddressDetail = anyOrNull(),
                deliveryLatitude = any(),
                deliveryLongitude = any(),
                phoneNumber = any(),
            ),
        ).willReturn(deliveryId)

        // when & then
        mockMvc
            .perform(
                post("/api/deliveries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/deliveries/2"))
            .andExpect(jsonPath("$.deliveryId").value(2))
            .andExpect(jsonPath("$.message").value("배달이 성공적으로 생성되었습니다."))
    }

    @Test
    @DisplayName("배달 완료 API - 성공")
    fun `배달 완료 API 성공`() {
        // given
        val deliveryId = 1L
        doNothing().whenever(deliveryService).completeDelivery(DeliveryId(deliveryId))

        // when & then
        mockMvc
            .perform(
                post("/api/deliveries/{deliveryId}/complete", deliveryId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.message").value("배달이 완료되었습니다."))
            .andDo(
                document(
                    "delivery-complete",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("deliveryId").description("배달 ID"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("완료된 배달 ID"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }
}
