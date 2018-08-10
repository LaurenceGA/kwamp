package co.nz.arm.wamp.router

import co.nz.arm.wamp.*
import co.nz.arm.wamp.messages.Abort
import co.nz.arm.wamp.messages.Goodbye
import co.nz.arm.wamp.messages.Hello
import co.nz.arm.wamp.messages.Welcome
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.ConcurrentHashMap

class Realm(val uri: Uri, private val messageSender: MessageSender = MessageSender()) {
    private val sessions = SessionSet(RandomIdGenerator())

    suspend fun join(connection: Connection) = startSession(connection)

    private suspend fun startSession(connection: Connection) = sessions.newSession(connection).apply {
        connection.forEachMessage {
            when(it) {
                is Hello -> throw ProtocolViolationException("Received Hello message after session established")
                is Welcome -> throw ProtocolViolationException("Receive Welcome message from client")
                is Abort -> connection.close("Abort from client")
                is Goodbye -> messageSender.sendGoodbye(connection)
            }
        }.invokeOnCompletion { exception ->
            when (exception) {
                is ProtocolViolationException -> messageSender.abort(connection, exception)
            }
            sessions.endSession(id)
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

class WampSession(val id: Long, private val connection: Connection) {
    init {
        launch {
            connection.send(Welcome(id, mapOf("agent" to "KWAMP", "roles" to mapOf("broker" to mapOf<String, Any?>(), "dealer" to mapOf()))))
        }
    }
}