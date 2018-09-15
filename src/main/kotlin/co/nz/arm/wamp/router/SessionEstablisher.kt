package co.nz.arm.wamp.router

import co.nz.arm.wamp.core.Connection
import co.nz.arm.wamp.core.NoSuchRealmException
import co.nz.arm.wamp.core.ProtocolViolationException
import co.nz.arm.wamp.core.Uri
import co.nz.arm.wamp.core.messages.Hello
import java.util.concurrent.ConcurrentHashMap

class SessionEstablisher(private val realms: ConcurrentHashMap<Uri, Realm>, private val connection: Connection, private val messageSender: MessageSender = MessageSender()) {
    suspend fun establish() {
        onExpectedHelloMessage {
            realms[it.realm]?.join(connection)
                    ?: throw NoSuchRealmException("Realm does not exist")
        }
    }

    private suspend fun onExpectedHelloMessage(action: suspend (message: Hello) -> Unit) {
        connection.onNextMessage {
            when (it) {
                is Hello -> action(it)
                else -> throw ProtocolViolationException("Didn't start with hello message")
            }
        }.invokeOnCompletion { throwable ->
            when (throwable) {
                is ProtocolViolationException -> messageSender.abort(connection, throwable)
                is NoSuchRealmException -> messageSender.abort(connection, throwable)
            }
        }
    }
}