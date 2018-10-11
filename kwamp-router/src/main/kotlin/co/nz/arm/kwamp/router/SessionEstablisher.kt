package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.Connection
import co.nz.arm.kwamp.core.NoSuchRealmException
import co.nz.arm.kwamp.core.ProtocolViolationException
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.messages.Hello
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