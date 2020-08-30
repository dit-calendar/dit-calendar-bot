package com.ditcalendar.bot.ditCalendarServer.data

import com.ditcalendar.bot.ditCalendarServer.data.core.Base
import com.ditcalendar.bot.ditCalendarServer.data.core.DateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

@Serializable
data class DitCalendar(var entryId: Long,
                       val title: String,
                       val description: String? = null,
                       @Serializable(with = DateSerializer::class)
                       val startDate: Date,
                       @Transient
                       var telegramTaskAssignments: TelegramTaskAssignments = listOf()) : Base()