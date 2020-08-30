package com.ditcalendar.bot.telegram.service

import com.ditcalendar.bot.ditCalendarServer.data.DitCalendar
import com.ditcalendar.bot.ditCalendarServer.data.TelegramTaskAfterUnassignment
import com.ditcalendar.bot.ditCalendarServer.data.TelegramTaskForUnassignment
import com.ditcalendar.bot.ditCalendarServer.data.core.Base
import com.ditcalendar.bot.service.assingAnnonCallbackCommand
import com.ditcalendar.bot.service.assingWithNameCallbackCommand
import com.ditcalendar.bot.service.reloadCallbackCommand
import com.ditcalendar.bot.service.unassignCallbackCommand
import com.ditcalendar.bot.telegram.formatter.parseError
import com.ditcalendar.bot.telegram.formatter.toMarkdown
import com.elbekD.bot.Bot
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.github.kittinunf.result.Result
import java.util.concurrent.CompletableFuture

const val parseMode = "MarkdownV2"
const val wrongRequestResponse = "request invalid"
const val reloadButtonText = "reload"
const val calendarReloadCallbackNotification = "calendar was reloaded"

fun Bot.commandResponse(response: Result<Base, Exception>, chatId: Long): CompletableFuture<Message> =
        when (response) {
            is Result.Success ->
                when (val responseObject = response.value) {
                    is DitCalendar -> {
                        val inlineButton = InlineKeyboardButton(reloadButtonText, callback_data = "$reloadCallbackCommand${responseObject.entryId}")
                        val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(inlineButton)))
                        sendMessage(chatId, responseObject.toMarkdown() + System.lineSeparator(), parseMode, true, markup = inlineKeyboardMarkup)
                    }
                    else ->
                        sendMessage(chatId, "internal server error", parseMode, true)
                }
            is Result.Failure ->
                sendMessage(chatId, parseError(response.error), parseMode, true)
        }


fun Bot.callbackResponse(response: Result<Base, Exception>, callbackQuery: CallbackQuery, originallyMessage: Message) {
    when (response) {
        is Result.Success ->
            when (val responseObject = response.value) {
                is DitCalendar -> {
                    val inlineButton = InlineKeyboardButton(reloadButtonText, callback_data = "$reloadCallbackCommand${responseObject.entryId}")
                    val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(inlineButton)))
                    val telegramAnswer = editMessageText(originallyMessage.chat.id, originallyMessage.message_id, text = responseObject.toMarkdown() + System.lineSeparator(),
                            parseMode = parseMode, disableWebPagePreview = true, markup = inlineKeyboardMarkup)

                    telegramAnswer.handleCallbackQuery(this, callbackQuery.id, calendarReloadCallbackNotification)
                }
                is TelegramTaskForUnassignment -> {
                    val inlineButton = InlineKeyboardButton("unassign me", callback_data = "$unassignCallbackCommand${responseObject.task.taskId}")
                    val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(inlineButton)))
                    val telegramAnswer = editMessageText(originallyMessage.chat.id, originallyMessage.message_id, text = responseObject.toMarkdown(),
                            parseMode = parseMode, disableWebPagePreview = true, markup = inlineKeyboardMarkup)

                    telegramAnswer.handleCallbackQuery(this, callbackQuery.id, null)
                }
                is TelegramTaskAfterUnassignment -> {
                    val telegramAnswer = editMessageText(originallyMessage.chat.id, originallyMessage.message_id, text = responseObject.toMarkdown(),
                            parseMode = parseMode)
                    telegramAnswer.handleCallbackQuery(this, callbackQuery.id, "successfully signed out")
                }
                else ->
                    answerCallbackQuery(callbackQuery.id, "internal server error", alert = true)
            }
        is Result.Failure ->
            answerCallbackQuery(callbackQuery.id, parseError(response.error), alert = true)
    }
}

fun Bot.deepLinkResponse(callbackOpts: String, chatId: Long) {
    val assignMeButton = InlineKeyboardButton("With telegram name", callback_data = assingWithNameCallbackCommand + callbackOpts)
    val anonAssignMeButton = InlineKeyboardButton("anonymous", callback_data = assingAnnonCallbackCommand + callbackOpts)
    val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(assignMeButton, anonAssignMeButton)))
    sendMessage(chatId, "Can I use your name?", parseMode, true, markup = inlineKeyboardMarkup)
}

private fun CompletableFuture<Message>.handleCallbackQuery(bot: Bot, calbackQueryId: String, callbackNotificationText: String?) {
    this.handle { _, throwable ->
        if (throwable == null || throwable.message!!.contains("Bad Request: message is not modified"))
            if (callbackNotificationText != null)
                bot.answerCallbackQuery(calbackQueryId, callbackNotificationText)
    }
}