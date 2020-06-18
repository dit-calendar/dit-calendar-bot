package com.ditcalendar.bot.service

import com.ditcalendar.bot.data.OnlyText
import com.ditcalendar.bot.data.WithInline
import com.ditcalendar.bot.data.core.Base
import com.ditcalendar.bot.formatter.parseResponse
import com.elbekD.bot.Bot
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.github.kittinunf.result.Result

fun sendMessage(response: Result<Base, Exception>, bot: Bot, msg: Message) {
    when (val result = parseResponse(response)) {
        is OnlyText ->
            bot.sendMessage(msg.chat.id, result.message, "MarkdownV2", true)
        is WithInline -> {
            val inlineButton = InlineKeyboardButton(result.callBackText, callback_data = result.callBackData)
            val inlineKeyboardMarkup = InlineKeyboardMarkup(listOf(listOf(inlineButton)))
            bot.sendMessage(msg.chat.id, result.message, "MarkdownV2", true, markup = inlineKeyboardMarkup)
        }
    }
}