package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.robot.vo.RouteResult

interface RobotMapClient {
    fun findRoute(waypoints: List<Location>): RouteResult
}
