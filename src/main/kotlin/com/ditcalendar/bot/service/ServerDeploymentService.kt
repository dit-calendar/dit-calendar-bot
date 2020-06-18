package com.ditcalendar.bot.service

import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.config.dit_calendar_deployment_url
import com.ditcalendar.bot.config.dit_calendar_server_url
import com.github.kittinunf.fuel.core.responseUnit
import com.github.kittinunf.fuel.httpGet

class ServerDeploymentService {

    private val config by config()
    private val ditCalendarUrl = config[dit_calendar_server_url]
    private val ditCalendarDeploymentUrl = config[dit_calendar_deployment_url]

    fun deployServer() {
        if (!healthCheck())
            wakeUpServer()
    }

    private fun healthCheck(): Boolean =
            "$ditCalendarUrl/"
                    .httpGet()
                    .response()
                    .second
                    .statusCode == 200

    private fun wakeUpServer() {
        "$ditCalendarDeploymentUrl/"
                .httpGet()
                .responseUnit()
    }
}