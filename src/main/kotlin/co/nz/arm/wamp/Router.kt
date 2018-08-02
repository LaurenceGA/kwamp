package co.nz.arm.wamp

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
    private val connectionRealm = ConcurrentHashMap<Connection, Realm?>()

    suspend fun newConnection(incoming: ReceiveChannel<String>, outgoing: SendChannel<String>, close: suspend (message: String) -> Unit) {
        println("Router setting up new connection!")
        val connection = Connection(incoming, outgoing, close).also { connectionRealm[it] = null }

        connection.forEachMessage { message ->
            when (message) {
                is Hello -> routeHello(connection, message)

            }
        }
    }

    private suspend fun routeHello(connection: Connection, message: Hello) {
        if (connectionRealm[connection] == null) joinRealm(connection, message.realm)
        else sendProtocolViolation(connection, "Received HELLO message after session was established.")
    }

    private suspend fun joinRealm(connection: Connection, realmURI: URI) = realms[realmURI]?.join(connection)
            ?: sendProtocolViolation(connection, "Realm does not exist")

    private suspend fun sendProtocolViolation(connection: Connection, message: String) = connection.apply {
        send(Abort("{}", message))
        close(message)
    }

    fun addRealm(realm: Realm) {
        if (realm.uri !in realms) realms[realm.uri] = realm
        else throw RuntimeException("Realm already exists")
    }
}

class Realm(val uri: URI) {
    private val sessions = ConcurrentSet<WampSession>()

    fun join(connection: Connection) = sessions.add(WampSession(connection))
}

class WampSession(private val connection: Connection)

class Connection(private val incoming: ReceiveChannel<String>, private val outgoing: SendChannel<String>, private val closeConnection: suspend (message: String) -> Unit) {
    suspend fun close(message: String) {
        outgoing.close()
        closeConnection(message)
    }

    suspend fun forEachMessage(action: suspend (Message) -> Unit) = launch {
        incoming.consumeEach { deserialize(it).also { action(it) } }
    }

    private fun deserialize(rawMessage: String): Message {
        val messageArray = Klaxon().parseArray<Any>(rawMessage)
        return MessageType.getFactory(messageArray!![0] as Int)?.invoke(messageArray.subList(1, messageArray.size))!!
    }

    suspend fun send(message: Message) {
        outgoing.send(message.toString())
    }
}