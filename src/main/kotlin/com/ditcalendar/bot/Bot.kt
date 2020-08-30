package com.ditcalendar.bot

import com.ditcalendar.bot.config.*
import com.ditcalendar.bot.ditCalendarServer.data.TelegramLink
import com.ditcalendar.bot.ditCalendarServer.endpoint.AuthEndpoint
import com.ditcalendar.bot.ditCalendarServer.endpoint.CalendarEndpoint
import com.ditcalendar.bot.ditCalendarServer.endpoint.TelegramAssignmentEndpoint
import com.ditcalendar.bot.domain.data.InvalidRequest
import com.ditcalendar.bot.service.CalendarService
import com.ditcalendar.bot.service.CommandExecution
import com.ditcalendar.bot.service.ServerDeploymentService
import com.ditcalendar.bot.service.assignDeepLinkCommand
import com.ditcalendar.bot.telegram.service.*
import com.elbekD.bot.Bot
import com.elbekD.bot.server
import com.elbekD.bot.types.Message
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.success

val helpMessage =
        """
            Mögliche Befehle sind
            /postcalendar {Hier Id einfügen} = Postet den Calendar mit der angegebenen ID
            /help = Zeigt alle Befehle an
        """.trimIndent()
const val BOT_COMMAND_POST_CALENDAR = "/postcalendar"

fun main(args: Array<String>) {


    val config by config()

    val token = config[telegram_token]
    val herokuApp = config[heroku_app_name]
    val commandExecution = CommandExecution(CalendarService(CalendarEndpoint(), TelegramAssignmentEndpoint(), AuthEndpoint()))
    val deploymentService = ServerDeploymentService()

    val bot = if (config[webhook_is_enabled]) {
        Bot.createWebhook(config[bot_name], token) {
            url = "https://$herokuApp.herokuapp.com/$token"

            /*
            Jetty server is used to listen to incoming request from Telegram servers.
            */
            server {
                host = "0.0.0.0"
                port = config[server_port]
            }
        }
    } else Bot.createPolling(config[bot_name], token)

    fun responseForDeeplinkAssignment(chatId: Long, opts: String) {
        if (opts.startsWith(assignDeepLinkCommand)) {
            val callbackOpts: String = opts.substringAfter(assignDeepLinkCommand)
            if (callbackOpts.isNotBlank()) {
                bot.deepLinkResponse(callbackOpts, chatId)
            } else {
                bot.commandResponse(Result.error(InvalidRequest()), chatId)
            }
        } else {
            bot.sendMessage(chatId, helpMessage)
        }
    }

    fun postCalendarCommand(msg: Message, opts: String?) {
        deploymentService.constraintsBeforeExecution(msg.message_id.toString()) {
            val response = commandExecution.executePublishCalendarCommand(opts)
            response.success { bot.deleteMessage(msg.chat.id, msg.message_id) }
            bot.commandResponse(response, msg.chat.id)
        }
    }

    bot.onCallbackQuery { callbackQuery ->
        deploymentService.constraintsBeforeExecution(callbackQuery.id) {
            val request = callbackQuery.data
            val originallyMessage = callbackQuery.message

            if (request == null || originallyMessage == null) {
                bot.answerCallbackQuery(callbackQuery.id, wrongRequestResponse)
            } else {
                val msgUser = callbackQuery.from
                val telegramLink = TelegramLink(originallyMessage.chat.id, msgUser.id, msgUser.username, msgUser.first_name)
                val response = commandExecution.executeCallback(telegramLink, request)

                bot.callbackResponse(response, callbackQuery, originallyMessage)
            }
        }
    }

    //for deeplinking
    bot.onCommand("/start") { msg, opts ->
        checkGlobalStateBeforeHandling(msg.message_id.toString()) {

            bot.deleteMessage(msg.chat.id, msg.message_id)
            val msgUser = msg.from
            //if message user is not set, we can't process
            if (msgUser == null) {
                bot.sendMessage(msg.chat.id, wrongRequestResponse)
            } else {
                if (opts != null)
                    responseForDeeplinkAssignment(msg.chat.id, opts)
                else
                    bot.sendMessage(msg.chat.id, helpMessage)
            }
        }
    }

    bot.onCommand("/help") { msg, _ ->
        checkGlobalStateBeforeHandling(msg.message_id.toString()) {
            bot.sendMessage(msg.chat.id, helpMessage)
        }
    }

    bot.onCommand(BOT_COMMAND_POST_CALENDAR) { msg, opts ->
        postCalendarCommand(msg, opts)
    }

    bot.onChannelPost { msg ->
        val msgText = msg.text
        if (msgText != null && msgText.startsWith(BOT_COMMAND_POST_CALENDAR))
            postCalendarCommand(msg, msgText.substringAfter(" "))
    }

    bot.start()
}