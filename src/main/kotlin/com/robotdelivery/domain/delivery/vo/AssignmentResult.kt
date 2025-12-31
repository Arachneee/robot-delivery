package com.robotdelivery.domain.delivery.vo

sealed class AssignmentResult {
    data object Success : AssignmentResult()

    sealed class Failure : AssignmentResult() {
        abstract val message: String

        data object RobotNotAvailable : Failure() {
            override val message: String = "새 로봇이 배차 가능한 상태가 아닙니다."
        }

        data object RouteNotAvailable : Failure() {
            override val message: String = "해당 로봇은 이 배달 경로를 수행할 수 없습니다."
        }

        data object IotStateNotFound : Failure() {
            override val message: String = "로봇 IoT 상태를 찾을 수 없습니다."
        }

        data object InsufficientBattery : Failure() {
            override val message: String = "로봇의 배터리가 부족합니다."
        }
    }
}
