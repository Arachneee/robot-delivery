package com.robotdelivery.view.delivery

import com.robotdelivery.application.command.DeliveryService
import com.robotdelivery.config.ControllerTestSupport
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.common.OrderNo
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.view.delivery.dto.CreateAdditionalDeliveryRequest
import com.robotdelivery.view.delivery.dto.CreateDeliveryRequest
import com.robotdelivery.view.delivery.dto.OrderItemRequest
import com.robotdelivery.view.delivery.dto.ReassignRobotRequest
import java.math.BigDecimal
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.given
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("DeliveryController 테스트")
class DeliveryControllerTest : ControllerTestSupport() {
    @MockitoBean
    private lateinit var deliveryService: DeliveryService

    @Test
    @DisplayName("배달 생성 API - 성공")
    fun `배달 생성 API 성공`() {
        val request =
            CreateDeliveryRequest(
                orderNo = "ORDER-001",
                pickupAddress = "서울시 중구 세종대로 110",
                pickupAddressDetail = "시청역 1번 출구",
                pickupLatitude = 37.5665,
                pickupLongitude = 126.9780,
                deliveryAddress = "서울시 강남구 테헤란로 521",
                deliveryAddressDetail = "삼성역 2번 출구",
                deliveryLatitude = 37.5087,
                deliveryLongitude = 127.0632,
                phoneNumber = "010-1234-5678",
                items = listOf(
                    OrderItemRequest(
                        name = "테스트 상품",
                        price = BigDecimal("10000"),
                        quantity = 1,
                        volume = 1.0,
                    ),
                ),
            )

        val deliveryId = DeliveryId(1L)
        given(deliveryService.createDelivery(any())).willReturn(deliveryId)

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
                        fieldWithPath("orderNo")
                            .type(JsonFieldType.STRING)
                            .description("주문 번호"),
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
                        fieldWithPath("items")
                            .type(JsonFieldType.ARRAY)
                            .description("주문 물품 목록"),
                        fieldWithPath("items[].name")
                            .type(JsonFieldType.STRING)
                            .description("물품 이름"),
                        fieldWithPath("items[].price")
                            .type(JsonFieldType.NUMBER)
                            .description("물품 가격"),
                        fieldWithPath("items[].quantity")
                            .type(JsonFieldType.NUMBER)
                            .description("물품 수량"),
                        fieldWithPath("items[].volume")
                            .type(JsonFieldType.NUMBER)
                            .description("물품 부피"),
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
        val request =
            CreateDeliveryRequest(
                orderNo = "ORDER-002",
                pickupAddress = "서울시 중구 세종대로 110",
                pickupAddressDetail = null,
                pickupLatitude = 37.5665,
                pickupLongitude = 126.9780,
                deliveryAddress = "서울시 강남구 테헤란로 521",
                deliveryAddressDetail = null,
                deliveryLatitude = 37.5087,
                deliveryLongitude = 127.0632,
                phoneNumber = "010-1234-5678",
                items = listOf(
                    OrderItemRequest(
                        name = "테스트 상품",
                        price = BigDecimal("10000"),
                        quantity = 1,
                        volume = 1.0,
                    ),
                ),
            )

        val deliveryId = DeliveryId(2L)
        given(deliveryService.createDelivery(any())).willReturn(deliveryId)

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
        val deliveryId = 1L
        doNothing().whenever(deliveryService).completeDelivery(DeliveryId(deliveryId))

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

    @Test
    @DisplayName("배송 시작 API - 성공")
    fun `배송 시작 API 성공`() {
        val deliveryId = 1L
        doNothing().whenever(deliveryService).startDelivery(DeliveryId(deliveryId))

        mockMvc
            .perform(
                post("/api/deliveries/{deliveryId}/start", deliveryId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.message").value("배송이 시작되었습니다."))
            .andDo(
                document(
                    "delivery-start",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("deliveryId").description("배달 ID"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("배송이 시작된 배달 ID"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("회수 완료 API - 성공")
    fun `회수 완료 API 성공`() {
        val deliveryId = 1L
        doNothing().whenever(deliveryService).completeReturn(DeliveryId(deliveryId))

        mockMvc
            .perform(
                post("/api/deliveries/{deliveryId}/complete-return", deliveryId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.message").value("회수가 완료되었습니다."))
            .andDo(
                document(
                    "delivery-complete-return",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("deliveryId").description("배달 ID"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("회수 완료된 배달 ID"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("배달 취소 API - 단순 취소 성공")
    fun `배달 취소 API 단순 취소 성공`() {
        val deliveryId = 1L
        whenever(deliveryService.cancelDelivery(DeliveryId(deliveryId))).thenReturn(false)

        mockMvc
            .perform(
                post("/api/deliveries/{deliveryId}/cancel", deliveryId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.requiresReturn").value(false))
            .andExpect(jsonPath("$.message").value("배달이 취소되었습니다."))
            .andDo(
                document(
                    "delivery-cancel",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("deliveryId").description("배달 ID"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("취소된 배달 ID"),
                        fieldWithPath("requiresReturn")
                            .type(JsonFieldType.BOOLEAN)
                            .description("물품 회수 필요 여부"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("배달 취소 API - 회수가 필요한 취소 성공")
    fun `배달 취소 API 회수가 필요한 취소 성공`() {
        val deliveryId = 1L
        whenever(deliveryService.cancelDelivery(DeliveryId(deliveryId))).thenReturn(true)

        mockMvc
            .perform(
                post("/api/deliveries/{deliveryId}/cancel", deliveryId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.requiresReturn").value(true))
            .andExpect(jsonPath("$.message").value("배달이 취소되었습니다. 물품이 픽업 위치로 회수됩니다."))
            .andDo(
                document(
                    "delivery-cancel-with-return",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("deliveryId").description("배달 ID"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("취소된 배달 ID"),
                        fieldWithPath("requiresReturn")
                            .type(JsonFieldType.BOOLEAN)
                            .description("물품 회수 필요 여부"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("배차 취소 API - 성공")
    fun `배차 취소 API 성공`() {
        val deliveryId = 1L
        doNothing().whenever(deliveryService).unassignRobot(DeliveryId(deliveryId))

        mockMvc
            .perform(
                post("/api/deliveries/{deliveryId}/unassign-robot", deliveryId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.message").value("배차가 취소되었습니다."))
            .andDo(
                document(
                    "delivery-unassign-robot",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("deliveryId").description("배달 ID"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("배차 취소된 배달 ID"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("배차 변경 API - 성공")
    fun `배차 변경 API 성공`() {
        val deliveryId = 1L
        val previousRobotId = 1L
        val newRobotId = 2L
        val request = ReassignRobotRequest(newRobotId = newRobotId)

        whenever(deliveryService.reassignRobot(DeliveryId(deliveryId), RobotId(newRobotId)))
            .thenReturn(RobotId(previousRobotId))

        mockMvc
            .perform(
                post("/api/deliveries/{deliveryId}/reassign-robot", deliveryId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.previousRobotId").value(previousRobotId))
            .andExpect(jsonPath("$.newRobotId").value(newRobotId))
            .andExpect(jsonPath("$.message").value("배차가 변경되었습니다."))
            .andDo(
                document(
                    "delivery-reassign-robot",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("deliveryId").description("배달 ID"),
                    ),
                    requestFields(
                        fieldWithPath("newRobotId")
                            .type(JsonFieldType.NUMBER)
                            .description("새로 배차할 로봇 ID"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("배차 변경된 배달 ID"),
                        fieldWithPath("previousRobotId")
                            .type(JsonFieldType.NUMBER)
                            .description("이전 로봇 ID (신규 배차인 경우 null)")
                            .optional(),
                        fieldWithPath("newRobotId")
                            .type(JsonFieldType.NUMBER)
                            .description("새 로봇 ID"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("배차 API - 로봇이 없는 상태에서 신규 배차 성공")
    fun `배차 API 신규 배차 성공`() {
        val deliveryId = 1L
        val newRobotId = 2L
        val request = ReassignRobotRequest(newRobotId = newRobotId)

        whenever(deliveryService.reassignRobot(DeliveryId(deliveryId), RobotId(newRobotId)))
            .thenReturn(null)

        mockMvc
            .perform(
                post("/api/deliveries/{deliveryId}/reassign-robot", deliveryId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.previousRobotId").isEmpty)
            .andExpect(jsonPath("$.newRobotId").value(newRobotId))
            .andExpect(jsonPath("$.message").value("로봇이 배차되었습니다."))
    }

    @Test
    @DisplayName("추가 배달 생성 API - 성공")
    fun `추가 배달 생성 API 성공`() {
        val orderNo = "ORDER-001"
        val request = CreateAdditionalDeliveryRequest(orderNo = orderNo)

        val deliveryId = DeliveryId(3L)
        given(deliveryService.createAdditionalDelivery(OrderNo(orderNo))).willReturn(deliveryId)

        mockMvc
            .perform(
                post("/api/deliveries/additional")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/deliveries/3"))
            .andExpect(jsonPath("$.deliveryId").value(3))
            .andExpect(jsonPath("$.orderNo").value(orderNo))
            .andExpect(jsonPath("$.message").value("추가 배달이 성공적으로 생성되었습니다."))
            .andDo(
                document(
                    "delivery-create-additional",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    requestFields(
                        fieldWithPath("orderNo")
                            .type(JsonFieldType.STRING)
                            .description("주문 번호"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("생성된 배달 ID"),
                        fieldWithPath("orderNo")
                            .type(JsonFieldType.STRING)
                            .description("주문 번호"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }
}
