package com.robotdelivery.domain.delivery.vo

import com.robotdelivery.domain.common.vo.Location
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded

@Embeddable
data class Destination(
    @Column(nullable = false)
    val address: String,
    @Column
    val addressDetail: String? = null,
    @Embedded
    val location: Location,
) {
    init {
        require(address.isNotBlank()) { "주소는 비어있을 수 없습니다." }
    }
}
