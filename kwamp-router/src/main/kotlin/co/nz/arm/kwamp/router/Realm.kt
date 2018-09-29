package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.*
import co.nz.arm.kwamp.core.messages.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class Realm(
    val uri: Uri,
    private val messageSender: MessageSender = MessageSender(),
    private val remoteProcedureHandler: RemoteProcedureHandler = RemoteProcedureHandler(
        messageSender,
        LinearIdGenerator()
    )
) {
    private val sessions = SessionSet(RandomIdGenerator())

    suspend fun join(connection: Connection) = startSession(connection)

    private suspend fun startSession(connection: Connection) = sessions.newSession(connection).apply {
        connection.forEachMessage {
            when (it) {
                is Hello -> throw ProtocolViolationException("Received Hello message after session established")
                is Welcome -> throw ProtocolViolationException("Receive Welcome message from client")
                is Abort -> connection.close("Abort from client")
                is Goodbye -> messageSender.sendGoodbye(connection)
                is Register -> remoteProcedureHandler.registerProcedure(connection, it)
                is Unregister -> remoteProcedureHandler.unregisterProcedure(connection, it)
                else -> throw NotImplementedError("Message type ${it.messageType} not implemented")
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

    fun newSession(connection: Connection) = idGenerator.newId().let { sessionId ->
        WampSession(sessionId, connection).also { sessions[sessionId] = it }
    }

    fun endSession(id: Long) {
        sessions.remove(id)
        idGenerator.releaseId(id)
    }
}

class WampSession(val id: Long, private val connection: Connection) {
    init {
        GlobalScope.launch {
            connection.send(
                Welcome(
                    id,
                    mapOf("agent" to "KWAMP", "roles" to mapOf("broker" to mapOf<String, Any?>(), "dealer" to mapOf()))
                )
            )
        }
    }
}