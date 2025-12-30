package com.robotdelivery.domain.order

import com.robotdelivery.domain.common.OrderId
import com.robotdelivery.domain.common.OrderNo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OrderRepository : JpaRepository<Order, Long> {
    fun findById(id: OrderId): Order? = findById(id.value).orElse(null)

    @Query("SELECT o FROM Order o WHERE o._orderNo = :orderNo")
    fun findByOrderNo(orderNo: String): Order?

    fun findByOrderNo(orderNo: OrderNo): Order? = findByOrderNo(orderNo.value)
}

fun OrderRepository.getById(id: OrderId): Order =
    findById(id) ?: throw IllegalArgumentException("주문을 찾을 수 없습니다: $id")

fun OrderRepository.getByOrderNo(orderNo: OrderNo): Order =
    findByOrderNo(orderNo) ?: throw IllegalArgumentException("주문을 찾을 수 없습니다: $orderNo")
