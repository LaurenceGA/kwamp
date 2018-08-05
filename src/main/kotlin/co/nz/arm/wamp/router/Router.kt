package co.nz.arm.wamp.router

import co.nz.arm.wamp.Connection
import co.nz.arm.wamp.URI
import co.nz.arm.wamp.messages.*
import com.beust.klaxon.Klaxon
import io.netty.util.internal.ConcurrentSet
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.ConcurrentHashMap

class Router {
    private val realms = ConcurrentHashMap<URI, Realm>()

    suspend fun newConnection(incoming: ReceiveChannel<String>, outgoing: SendChannel<String>, close: suspend (message: String) -> Unit) {
        println("Router setting up new connection!")
        val connection = Connection(incoming, outgoing, close)
        SessionEstablisher(realms, connection).establish()
    }

    private suspend fun sendProtocolViolation(connection: Connection, message: String) = connection.apply {
        send(Abort("{}", message))
        close(message)
    }

    fun addRealm(realm: Realm) {
        if (realm.uri !in realms) realms[realm.uri] = realm
        else throw RuntimeException("Realm already exists")
    }
}