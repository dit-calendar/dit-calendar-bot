package com.ditcalendar.bot.service

import com.ditcalendar.bot.ditCalendarServer.data.DitCalendar
import com.ditcalendar.bot.ditCalendarServer.data.TelegramLink
import com.ditcalendar.bot.ditCalendarServer.data.TelegramTaskAfterUnassignment
import com.ditcalendar.bot.ditCalendarServer.data.TelegramTaskForUnassignment
import com.ditcalendar.bot.ditCalendarServer.endpoint.AuthEndpoint
import com.ditcalendar.bot.ditCalendarServer.endpoint.CalendarEndpoint
import com.ditcalendar.bot.ditCalendarServer.endpoint.TelegramAssignmentEndpoint
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map


class CalendarService(private val calendarEndpoint: CalendarEndpoint,
                      private val taskEndpoint: TelegramAssignmentEndpoint,
                      private val authEndpoint: AuthEndpoint) {

    fun getCalendarAndTask(calendarId: Long): Result<DitCalendar, Exception> {
        val tokenResul = authEndpoint.getToken()
        val calendarResult = tokenResul.flatMap { calendarEndpoint.readCalendar(calendarId, it) }
        val tasksResulst = tokenResul.flatMap { taskEndpoint.readTasks(calendarId, it) }

        return calendarResult.flatMap { calendar ->
            tasksResulst.map {
                calendar.apply { telegramTaskAssignments = it }
            }
        }
    }

    fun assignUserToTask(taskId: Long, telegramLink: TelegramLink): Result<TelegramTaskForUnassignment, Exception> =
            authEndpoint.getToken()
                    .flatMap { taskEndpoint.assignUserToTask(taskId, telegramLink, it) }

    fun unassignUserFromTask(taskId: Long, telegramLink: TelegramLink): Result<TelegramTaskAfterUnassignment, Exception> {
        return authEndpoint.getToken()
                .flatMap { taskEndpoint.unassignUserFromTask(taskId, telegramLink, it) }
    }
}