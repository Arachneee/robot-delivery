package com.robotdelivery.infrastructure.persistence.converter

import com.robotdelivery.domain.common.vo.Volume
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class VolumeConverter : AttributeConverter<Volume?, Double?> {
    override fun convertToDatabaseColumn(attribute: Volume?): Double? = attribute?.value

    override fun convertToEntityAttribute(dbData: Double?): Volume? = dbData?.let { Volume(it) }
}
