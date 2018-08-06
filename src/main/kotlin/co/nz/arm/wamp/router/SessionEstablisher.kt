package co.nz.arm.wamp.router

import co.nz.arm.wamp.Connection
import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.messages.Hello
import java.util.concurrent.ConcurrentHashMap

class SessionEstablisher(private val realms: ConcurrentHashMap<Uri, Realm>, private val connection: Connection) {
    suspend fun establish() {
        onExpectedHelloMessage {
            realms[it.realm]?.join(connection)
                    ?: throw RuntimeException("Realm does not exist")
        }
    }

    private suspend fun onExpectedHelloMessage(action: suspend (message: Hello) -> Unit) {
        connection.onNextMessage {
            when(it) {
                is Hello -> action(it)
                else -> throw RuntimeException("Didn't start with hello message")
            }
        }
    }
}