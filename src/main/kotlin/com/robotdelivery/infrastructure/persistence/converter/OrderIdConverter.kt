package com.robotdelivery.infrastructure.persistence.converter

import com.robotdelivery.domain.common.OrderId
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class OrderIdConverter : AttributeConverter<OrderId?, Long?> {
    override fun convertToDatabaseColumn(attribute: OrderId?): Long? = attribute?.value

    override fun convertToEntityAttribute(dbData: Long?): OrderId? = dbData?.let { OrderId(it) }
}

