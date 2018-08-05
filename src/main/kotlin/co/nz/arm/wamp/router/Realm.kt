package co.nz.arm.wamp.router

import co.nz.arm.wamp.*
import co.nz.arm.wamp.messages.Welcome
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.ConcurrentHashMap

class Realm(val uri: URI) {
    private val sessions = SessionSet(RandomIdGenerator())

    suspend fun join(connection: Connection) = startSession(connection)

    private suspend fun startSession(connection: Connection) = sessions.newSession(connection).apply {
        connection.forEachMessage {
            println(it)
        }
    }
}

class SessionSet(private val idGenerator: WampIdGenerator) {
    private val sessions = ConcurrentHashMap<Long, WampSession>()

    fun newSession(connection: Connection) = idGenerator.newId().let {sessionId ->
        WampSession(sessionId, connection).also { sessions[sessionId] = it }
    }

    fun endSession(id: Long) {
        sessions.remove(id)
        idGenerator.releaseId(id)
    }
}

class WampSession(id: Long, val connection: Connection) {
    init {
        launch {
            connection.send(Welcome(id, ""))
        }
    }
}