package com.robotdelivery.domain.robot

import com.robotdelivery.domain.common.Location
import com.robotdelivery.domain.common.RobotId
import com.robotdelivery.domain.robot.event.RobotEvent

typealias RobotEventFactory = (RobotId, Location) -> RobotEvent

