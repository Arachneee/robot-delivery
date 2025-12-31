package com.robotdelivery.domain.order

import com.robotdelivery.domain.common.vo.Volume
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.math.BigDecimal

@Embeddable
data class OrderItem(
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false, precision = 19, scale = 2)
    val price: BigDecimal,
    @Column(nullable = false)
    val quantity: Int,
    @Column(nullable = false)
    val volume: Double,
) {
    init {
        require(name.isNotBlank()) { "물품 이름은 비어있을 수 없습니다." }
        require(price >= BigDecimal.ZERO) { "가격은 0 이상이어야 합니다." }
        require(quantity > 0) { "수량은 1 이상이어야 합니다." }
        require(volume >= 0) { "부피는 0 이상이어야 합니다." }
    }

    fun calculateTotalVolume(): Volume = Volume(volume) * quantity
}
