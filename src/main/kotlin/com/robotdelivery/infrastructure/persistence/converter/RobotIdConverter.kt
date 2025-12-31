package com.robotdelivery.infrastructure.persistence.converter

import com.robotdelivery.domain.common.vo.RobotId
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class RobotIdConverter : AttributeConverter<RobotId?, Long?> {
    override fun convertToDatabaseColumn(attribute: RobotId?): Long? = attribute?.value

    override fun convertToEntityAttribute(dbData: Long?): RobotId? = dbData?.let { RobotId(it) }
}
