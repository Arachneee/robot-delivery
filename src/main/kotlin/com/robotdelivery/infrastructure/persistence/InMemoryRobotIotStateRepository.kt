package com.robotdelivery.infrastructure.persistence

import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.RobotIotState
import com.robotdelivery.domain.robot.RobotIotStateRepository
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryRobotIotStateRepository : RobotIotStateRepository {
    private val store = ConcurrentHashMap<RobotId, RobotIotState>()

    override fun save(state: RobotIotState) {
        store[state.robotId] = state
    }

    override fun findById(robotId: RobotId): RobotIotState? = store[robotId]

    override fun findAll(): List<RobotIotState> = store.values.toList()

    override fun deleteById(robotId: RobotId) {
        store.remove(robotId)
    }

    fun deleteAll() {
        store.clear()
    }
}
