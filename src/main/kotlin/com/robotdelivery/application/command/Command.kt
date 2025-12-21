package com.robotdelivery.application.command

interface Command

interface CommandHandler<C : Command, R> {
    fun handle(command: C): R
}

