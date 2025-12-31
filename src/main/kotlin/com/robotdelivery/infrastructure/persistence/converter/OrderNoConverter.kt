package com.robotdelivery.infrastructure.persistence.converter

import com.robotdelivery.domain.common.vo.OrderNo
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class OrderNoConverter : AttributeConverter<OrderNo?, String?> {
    override fun convertToDatabaseColumn(attribute: OrderNo?): String? = attribute?.value

    override fun convertToEntityAttribute(dbData: String?): OrderNo? = dbData?.let { OrderNo(it) }
}
