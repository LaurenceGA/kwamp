package co.nz.arm.wamp.router

import co.nz.arm.wamp.Connection
import co.nz.arm.wamp.URI
import co.nz.arm.wamp.messages.Hello
import co.nz.arm.wamp.messages.Welcome
import java.util.concurrent.ConcurrentHashMap

class SessionEstablisher(private val realms: ConcurrentHashMap<URI, Realm>, private val connection: Connection) {
    suspend fun establish() {
        onExpectedHelloMessage {
            realms[it.realm]?.join(connection)
                    ?: throw RuntimeException("Realm does not exist")
            connection.send(Welcome(1, ""))
        }
    }

    private suspend fun onExpectedHelloMessage(action: suspend (message: Hello) -> Unit) {
        connection.onNextMessage {
            if (it is Hello) action(it)
            else throw RuntimeException("Didn't start with hello message")
        }
    }
}