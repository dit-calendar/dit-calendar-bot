package com.ditcalendar.bot.ditCalendarServer.data.core

import kotlinx.serialization.Serializable

@Serializable
abstract class Base(var version: Int = 0)