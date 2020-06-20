package com.ditcalendar.bot.service

import com.ditcalendar.bot.config.config
import com.ditcalendar.bot.config.dit_calendar_deployment_url
import com.ditcalendar.bot.endpoint.MonitoringEndpoint
import com.github.kittinunf.fuel.core.responseUnit
import com.github.kittinunf.fuel.httpGet
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ServerDeploymentService {

    private val config by config()
    private val ditCalendarDeploymentUrl = config[dit_calendar_deployment_url]

    private val monitorEndpoint = MonitoringEndpoint()

    fun deployServer() {
        if (!monitorEndpoint.healthCheck())
            GlobalScope.launch {
                wakeUpServer()
            }
    }

    private fun wakeUpServer() {
        "$ditCalendarDeploymentUrl/"
                .httpGet()
                .responseUnit()
    }

    inline fun constraintsBeforeExecution(msgId: String, requestHandling: () -> Unit) {
        checkGlobalStateBeforeHandling(msgId) {
            deployBeforeExecution(requestHandling)
        }
    }

    inline fun deployBeforeExecution(requestHandling: () -> Unit) {
        deployServer()

        requestHandling()
    }
}

inline fun checkGlobalStateBeforeHandling(msgId: String, requestHandling: () -> Unit) {
    if (globalStateForFirstMessage == null || globalStateForFirstMessage != msgId) {
        globalStateForFirstMessage = msgId
        requestHandling()
    }
}

var globalStateForFirstMessage: String? = null