package com.ditcalendar.bot.ditCalendarServer.endpoint

import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.config.dit_calendar_server_url
import com.github.kittinunf.fuel.httpGet


class MonitoringEndpoint {

    private val config by config()
    private val ditCalendarUrl = config[dit_calendar_server_url]

    fun healthCheck(): Boolean =
            "$ditCalendarUrl/"
                    .httpGet()
                    .response()
                    .second
                    .statusCode == 200
}