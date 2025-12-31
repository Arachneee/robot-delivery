package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.vo.Location
import com.robotdelivery.domain.common.vo.RobotId
import com.robotdelivery.domain.robot.event.RobotEvent

typealias RobotEventFactory = (RobotId, Location) -> RobotEvent
