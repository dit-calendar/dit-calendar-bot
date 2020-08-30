package com.ditcalendar.bot.ditCalendarServer.data

import com.ditcalendar.bot.ditCalendarServer.data.core.Base
import kotlinx.serialization.Serializable

typealias TelegramTaskAssignments = List<TelegramTaskAssignment>

@Serializable
sealed class TelegramTaskAssignment : Base() {
    abstract val task: Task
    abstract val assignedUsers: TelegramLinks
}

@Serializable
class TelegramTaskForAssignment(override val task: TaskForAssignment, override val assignedUsers: TelegramLinks) : TelegramTaskAssignment()

@Serializable
class TelegramTaskForUnassignment(override val task: TaskForUnassignment, override val assignedUsers: TelegramLinks) : TelegramTaskAssignment()

@Serializable
class TelegramTaskAfterUnassignment(override val task: TaskAfterUnassignment, override val assignedUsers: TelegramLinks) : TelegramTaskAssignment()