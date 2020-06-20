package com.ditcalendar.bot.error

sealed class DitBotError(description: String) : RuntimeException(description)

class InvalidRequest : DitBotError("request invalid")
class ServerNotReachable: DitBotError("server need to startup. try again")
