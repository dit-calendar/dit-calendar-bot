package com.ditcalendar.bot.service

import com.ditcalendar.bot.ditCalendarServer.data.DitCalendar
import com.ditcalendar.bot.ditCalendarServer.data.TelegramLink
import com.ditcalendar.bot.ditCalendarServer.data.core.Base
import com.ditcalendar.bot.domain.data.InvalidRequest
import com.github.kittinunf.result.Result

const val assignDeepLinkCommand = "assign_"
const val unassignCallbackCommand = "unassign_"
const val reloadCallbackCommand = "reloadCalendar_"
const val assingWithNameCallbackCommand = "assignme_"
const val assingAnnonCallbackCommand = "assignmeAnnon_"

class CommandExecution(private val calendarService: CalendarService) {

    fun executeCallback(telegramLink: TelegramLink, callbaBackData: String): Result<Base, Exception> =
            if (callbaBackData.startsWith(unassignCallbackCommand)) {
                val taskId: Long? = callbaBackData.substringAfter(unassignCallbackCommand).toLongOrNull()
                if (taskId != null)
                    calendarService.unassignUserFromTask(taskId, telegramLink)
                else
                    Result.error(InvalidRequest())
            } else if (callbaBackData.startsWith(reloadCallbackCommand)) {
                reloadCalendar(callbaBackData.substringAfter(reloadCallbackCommand))
            } else if (callbaBackData.startsWith(assingWithNameCallbackCommand)) {
                executeTaskAssignmentCommand(telegramLink, callbaBackData)
            } else if (callbaBackData.startsWith(assingAnnonCallbackCommand)) {
                executeTaskAssignmentCommand(telegramLink.copy(firstName = null, userName = null), callbaBackData)
            } else
                Result.error(InvalidRequest())

    fun executeTaskAssignmentCommand(telegramLink: TelegramLink, opts: String): Result<Base, Exception> {
        val taskId: Long? = opts.substringAfter("_").toLongOrNull()
        return if (taskId != null)
            calendarService.assignUserToTask(taskId, telegramLink)
        else
            Result.error(InvalidRequest())
    }

    fun executePublishCalendarCommand(opts: String?): Result<Base, Exception> {
        val calendarId: Long? = opts?.toLongOrNull()
        return if (calendarId != null)
            calendarService.getCalendarAndTask(calendarId)
        else
            Result.error(InvalidRequest())
    }

    private fun reloadCalendar(opts: String): Result<DitCalendar, Exception> {
        val calendarId: Long? = opts.toLongOrNull()
        return if (calendarId != null)
            calendarService.getCalendarAndTask(calendarId)
        else
            Result.error(InvalidRequest())
    }
}
