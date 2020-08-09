package com.ditcalendar.bot.telegram.formatter

import com.ditcalendar.bot.ditCalendarServer.data.DitCalendar
import com.ditcalendar.bot.ditCalendarServer.data.TelegramTaskAfterUnassignment
import com.ditcalendar.bot.ditCalendarServer.data.TelegramTaskForAssignment
import com.ditcalendar.bot.ditCalendarServer.data.TelegramTaskForUnassignment
import com.ditcalendar.bot.ditCalendarServer.data.core.Base
import com.ditcalendar.bot.domain.data.DitBotError
import com.ditcalendar.bot.domain.data.InvalidRequest
import com.ditcalendar.bot.domain.data.ServerNotReachable
import com.ditcalendar.bot.service.reloadCallbackCommand
import com.ditcalendar.bot.telegram.data.InlineMessageResponse
import com.ditcalendar.bot.telegram.data.MessageResponse
import com.ditcalendar.bot.telegram.data.TelegramResponse
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.serialization.json.JsonDecodingException

fun parseResponse(result: Result<Base, Exception>): TelegramResponse =
        when (result) {
            is Result.Success -> parseSuccess(result.value)
            is Result.Failure -> {
                result.error.printStackTrace()
                parseError(result.error)
            }
        }

private fun parseSuccess(result: Base): TelegramResponse =
        when (result) {
            is DitCalendar ->
                InlineMessageResponse(result.toMarkdown() + System.lineSeparator(), "reload", "$reloadCallbackCommand${result.entryId}", "calendar wurde neugeladen")
            is TelegramTaskForUnassignment ->
                InlineMessageResponse(result.toMarkdown(),
                        "unassign me", "unassign_${result.task.taskId}", null)
            is TelegramTaskForAssignment ->
                MessageResponse("nicht implementiert", null)
            is TelegramTaskAfterUnassignment ->
                MessageResponse(result.toMarkdown(), "erfolgreich ausgetragen")
            else ->
                MessageResponse("interner server Fehler", null)
        }

private fun parseError(error: Exception): TelegramResponse =
        MessageResponse(when (error) {
            is FuelError -> {
                when (error.response.statusCode) {
                    401 -> "Bot is missing necessary access rights"
                    403 -> "Bot is missing necessary access rights"
                    404 -> "calendar or task not found"
                    503 -> "server not reachable, try again in a moment"
                    else -> if (error.cause is JsonDecodingException) {
                        "unexpected server response"
                    } else if (error.message != null)
                        "Error: " + error.message.toString()
                    else "unkown Error"
                }
            }
            is DitBotError -> {
                when (error) {
                    is InvalidRequest -> "incorrect request to server"
                    is ServerNotReachable -> "server need to startup, try again"
                }
            }
            else -> "unknown error"
        }.withMDEscape(), null)
