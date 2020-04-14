package com.ditcalendar.bot.service

import com.ditcalendar.bot.data.DitCalendar
import com.ditcalendar.bot.data.TaskAfterUnassignment
import com.ditcalendar.bot.data.TaskForUnassignment
import com.ditcalendar.bot.data.TelegramLink
import com.ditcalendar.bot.data.core.Base
import com.ditcalendar.bot.endpoint.AuthEndpoint
import com.ditcalendar.bot.endpoint.CalendarEndpoint
import com.ditcalendar.bot.endpoint.TaskEndpoint
import com.ditcalendar.bot.error.InvalidRequest
import com.ditcalendar.bot.error.UnassigmentError
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class DitCalendarService(private val calendarEndpoint: CalendarEndpoint,
                         private val taskEndpoint: TaskEndpoint,
                         private val authEndpoint: AuthEndpoint) {

    fun executeUnassignCallback(telegramLink: TelegramLink, opts: String): Result<TaskAfterUnassignment, Exception> {
        val taskId: Long? = opts.toLongOrNull()
        return if (taskId != null)
            unassignUserFromTask(taskId, telegramLink)
        else
            Result.error(InvalidRequest())
    }


    fun executeTaskAssignmentCommand(telegramLink: TelegramLink, opts: String): Result<Base, Exception> {
        val taskId: Long? = opts.toLongOrNull()
        return if (taskId != null)
            assignUserToTask(taskId, telegramLink)
        else
            Result.error(InvalidRequest())
    }

    fun executePublishCalendarCommand(opts: String?): Result<Base, Exception> {
        val calendarId: Long? = opts?.toLongOrNull()
        return if (calendarId != null)
            getCalendarAndTask(calendarId)
        else
            Result.error(InvalidRequest())
    }


    private fun getCalendarAndTask(calendarId: Long): Result<DitCalendar, Exception> {
        val calendarResult = calendarEndpoint.readCalendar(calendarId)
        val tokenResul = authEndpoint.getToken()
        val tasksResulst = tokenResul.flatMap { taskEndpoint.readTasks(calendarId, it) }

        return calendarResult.flatMap { calendar ->
            tasksResulst.map {
                calendar.apply { tasks = it }
            }
        }
    }

    private fun assignUserToTask(taskId: Long, telegramLink: TelegramLink): Result<TaskForUnassignment, Exception> =
            authEndpoint.getToken()
                    .flatMap { taskEndpoint.assignUserToTask(taskId, telegramLink, it) }

    private fun unassignUserFromTask(taskId: Long, telegramLink: TelegramLink): Result<TaskAfterUnassignment, Exception> {
        return authEndpoint.getToken()
                .flatMap { taskEndpoint.unassignUserFromTask(taskId, telegramLink, it) }
                .fold({ Result.Success(it) }, { Result.error(UnassigmentError()) })
    }
}