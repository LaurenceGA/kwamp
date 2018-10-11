package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.NoSuchRealmException
import com.laurencegarmstrong.kwamp.core.ProtocolViolationException
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Hello
import java.util.concurrent.ConcurrentHashMap

class SessionEstablisher(
    private val realms: ConcurrentHashMap<Uri, Realm>,
    private val connection: Connection,
    private val messageSender: MessageSender = MessageSender()
) {
    suspend fun establish() {
        connection.withNextMessage { message: Hello ->
            realms[message.realm]?.join(connection)
                ?: throw NoSuchRealmException("Realm '${message.realm.text}' does not exist")
        }.invokeOnCompletion { throwable ->
            when (throwable) {
                is ProtocolViolationException -> messageSender.sendAbort(connection, throwable)
                is NoSuchRealmException -> messageSender.sendAbort(connection, throwable)
                else -> throwable?.run { printStackTrace() }
            }
        }
    }
}