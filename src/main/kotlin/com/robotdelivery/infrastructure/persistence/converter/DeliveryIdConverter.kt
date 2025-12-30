package com.robotdelivery.infrastructure.persistence.converter

import com.robotdelivery.domain.common.DeliveryId
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class DeliveryIdConverter : AttributeConverter<DeliveryId?, Long?> {
    override fun convertToDatabaseColumn(attribute: DeliveryId?): Long? = attribute?.value

    override fun convertToEntityAttribute(dbData: Long?): DeliveryId? = dbData?.let { DeliveryId(it) }
}

