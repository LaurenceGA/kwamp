package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.Hello
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class SessionEstablisher(
    private val realms: ConcurrentHashMap<Uri, Realm>,
    private val connection: Connection,
    private val messageSender: MessageSender = MessageSender()
) {
    fun establish() = GlobalScope.launch {
        var connected: Boolean = false
        do {
            connection.withNextMessage { message: Hello ->
                realms[message.realm]?.join(connection)
                    ?: throw NoSuchRealmException("Realm '${message.realm.text}' does not exist")
                connected = true
            }.apply {
                invokeOnCompletion { throwable ->
                    when (throwable) {
                        is ProtocolViolationException -> messageSender.sendAbort(connection, throwable)
                        is NoSuchRealmException -> messageSender.sendAbort(connection, throwable)
                        is InvalidUriErrorException -> messageSender.sendExceptionError(connection, throwable)
                        else -> throwable?.run { printStackTrace() }
                    }
                }
            }.join()
        } while (!connected)
    }
}