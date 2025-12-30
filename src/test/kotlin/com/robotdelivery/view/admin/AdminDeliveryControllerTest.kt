package com.robotdelivery.view.admin

import com.robotdelivery.application.command.DeliveryService
import com.robotdelivery.application.command.vo.ChangeStatusResult
import com.robotdelivery.config.ControllerTestSupport
import com.robotdelivery.domain.common.DeliveryId
import com.robotdelivery.domain.delivery.DeliveryStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("AdminDeliveryController 테스트")
class AdminDeliveryControllerTest : ControllerTestSupport() {
    @MockitoBean
    private lateinit var deliveryService: DeliveryService

    @Test
    @DisplayName("상태 변경 API - PENDING에서 CANCELED로 변경 성공")
    fun `상태 변경 API PENDING에서 CANCELED로 변경 성공`() {
        val deliveryId = 1L
        val result =
            ChangeStatusResult(
                previousStatus = DeliveryStatus.PENDING,
                currentStatus = DeliveryStatus.CANCELED,
            )

        whenever(
            deliveryService.changeStatus(
                deliveryId = DeliveryId(deliveryId),
                targetStatus = DeliveryStatus.CANCELED,
            ),
        ).thenReturn(result)

        mockMvc
            .perform(
                post("/api/admin/deliveries/{deliveryId}/change-status?targetStatus=CANCELED", deliveryId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.previousStatus").value("PENDING"))
            .andExpect(jsonPath("$.currentStatus").value("CANCELED"))
            .andExpect(
                jsonPath("$.message").value("상태가 PENDING에서 CANCELED(으)로 변경되었습니다."),
            ).andDo(
                document(
                    "admin-delivery-change-status",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("deliveryId").description("배달 ID"),
                    ),
                    queryParameters(
                        parameterWithName("targetStatus").description("변경할 목표 상태"),
                    ),
                    responseFields(
                        fieldWithPath("deliveryId")
                            .type(JsonFieldType.NUMBER)
                            .description("배달 ID"),
                        fieldWithPath("previousStatus")
                            .type(JsonFieldType.STRING)
                            .description("변경 전 상태"),
                        fieldWithPath("currentStatus")
                            .type(JsonFieldType.STRING)
                            .description("변경 후 상태"),
                        fieldWithPath("message")
                            .type(JsonFieldType.STRING)
                            .description("응답 메시지"),
                    ),
                ),
            )
    }

    @Test
    @DisplayName("상태 변경 API - ASSIGNED에서 PICKUP_ARRIVED로 변경 성공")
    fun `상태 변경 API ASSIGNED에서 PICKUP_ARRIVED로 변경 성공`() {
        val deliveryId = 2L
        val result =
            ChangeStatusResult(
                previousStatus = DeliveryStatus.ASSIGNED,
                currentStatus = DeliveryStatus.PICKUP_ARRIVED,
            )

        whenever(
            deliveryService.changeStatus(
                deliveryId = DeliveryId(deliveryId),
                targetStatus = DeliveryStatus.PICKUP_ARRIVED,
            ),
        ).thenReturn(result)

        mockMvc
            .perform(
                post("/api/admin/deliveries/{deliveryId}/change-status", deliveryId)
                    .param("targetStatus", "PICKUP_ARRIVED"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.previousStatus").value("ASSIGNED"))
            .andExpect(jsonPath("$.currentStatus").value("PICKUP_ARRIVED"))
    }

    @Test
    @DisplayName("상태 변경 API - PICKING_UP에서 DELIVERING으로 변경 성공")
    fun `상태 변경 API PICKING_UP에서 DELIVERING으로 변경 성공`() {
        val deliveryId = 3L
        val result =
            ChangeStatusResult(
                previousStatus = DeliveryStatus.PICKING_UP,
                currentStatus = DeliveryStatus.DELIVERING,
            )

        whenever(
            deliveryService.changeStatus(
                deliveryId = DeliveryId(deliveryId),
                targetStatus = DeliveryStatus.DELIVERING,
            ),
        ).thenReturn(result)

        mockMvc
            .perform(
                post("/api/admin/deliveries/{deliveryId}/change-status", deliveryId)
                    .param("targetStatus", "DELIVERING"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.previousStatus").value("PICKING_UP"))
            .andExpect(jsonPath("$.currentStatus").value("DELIVERING"))
    }

    @Test
    @DisplayName("상태 변경 API - DROPPING_OFF에서 COMPLETED로 변경 성공")
    fun `상태 변경 API DROPPING_OFF에서 COMPLETED로 변경 성공`() {
        val deliveryId = 4L
        val result =
            ChangeStatusResult(
                previousStatus = DeliveryStatus.DROPPING_OFF,
                currentStatus = DeliveryStatus.COMPLETED,
            )

        whenever(
            deliveryService.changeStatus(
                deliveryId = DeliveryId(deliveryId),
                targetStatus = DeliveryStatus.COMPLETED,
            ),
        ).thenReturn(result)

        mockMvc
            .perform(
                post("/api/admin/deliveries/{deliveryId}/change-status", deliveryId)
                    .param("targetStatus", "COMPLETED"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.previousStatus").value("DROPPING_OFF"))
            .andExpect(jsonPath("$.currentStatus").value("COMPLETED"))
    }

    @Test
    @DisplayName("상태 변경 API - DELIVERING에서 RETURNING으로 변경 성공")
    fun `상태 변경 API DELIVERING에서 RETURNING으로 변경 성공`() {
        val deliveryId = 5L
        val result =
            ChangeStatusResult(
                previousStatus = DeliveryStatus.DELIVERING,
                currentStatus = DeliveryStatus.RETURNING,
            )

        whenever(
            deliveryService.changeStatus(
                deliveryId = DeliveryId(deliveryId),
                targetStatus = DeliveryStatus.RETURNING,
            ),
        ).thenReturn(result)

        mockMvc
            .perform(
                post("/api/admin/deliveries/{deliveryId}/change-status", deliveryId)
                    .param("targetStatus", "RETURNING"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.previousStatus").value("DELIVERING"))
            .andExpect(jsonPath("$.currentStatus").value("RETURNING"))
    }

    @Test
    @DisplayName("상태 변경 API - RETURNING_OFF에서 RETURN_COMPLETED로 변경 성공")
    fun `상태 변경 API RETURNING_OFF에서 RETURN_COMPLETED로 변경 성공`() {
        val deliveryId = 6L
        val result =
            ChangeStatusResult(
                previousStatus = DeliveryStatus.RETURNING_OFF,
                currentStatus = DeliveryStatus.RETURN_COMPLETED,
            )

        whenever(
            deliveryService.changeStatus(
                deliveryId = DeliveryId(deliveryId),
                targetStatus = DeliveryStatus.RETURN_COMPLETED,
            ),
        ).thenReturn(result)

        mockMvc
            .perform(
                post("/api/admin/deliveries/{deliveryId}/change-status", deliveryId)
                    .param("targetStatus", "RETURN_COMPLETED"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.deliveryId").value(deliveryId))
            .andExpect(jsonPath("$.previousStatus").value("RETURNING_OFF"))
            .andExpect(jsonPath("$.currentStatus").value("RETURN_COMPLETED"))
    }
}
