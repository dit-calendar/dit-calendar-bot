package com.ditcalendar.bot.domain.data

sealed class DitBotError(description: String) : RuntimeException(description)

class InvalidRequest : DitBotError("request invalid")
class ServerNotReachable : DitBotError("server need to startup. try again")
