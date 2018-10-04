package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.*
import co.nz.arm.kwamp.core.messages.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class Realm(
    val uri: Uri
) {
    private val messageSender: MessageSender = MessageSender()
    private val remoteProcedureHandler: RemoteProcedureHandler = RemoteProcedureHandler(
        messageSender,
        LinearIdGenerator(),
        RandomIdGenerator()
    )
    private val subscriptionHandler: SubscriptionHandler = SubscriptionHandler(messageSender, RandomIdGenerator())

    private val sessions = SessionSet(RandomIdGenerator())

    suspend fun join(connection: Connection) = startSession(connection)

    private suspend fun startSession(connection: Connection) = sessions.newSession(connection).apply {
        connection.forEachMessage {
            try {
                handleConnectionMessage(it, this)
            } catch (nonFatalError: WampErrorException) {
                messageSender.sendExceptionError(connection, nonFatalError)
            }
        }.invokeOnCompletion { fatalException ->
            when (fatalException) {
                is ProtocolViolationException -> messageSender.sendAbort(connection, fatalException)
                else -> fatalException?.run { printStackTrace() }
            }
            sessions.endSession(id)
        }
    }

    private suspend fun handleConnectionMessage(
        message: Message,
        session: WampSession
    ) {
        when (message) {
            is Hello -> throw ProtocolViolationException("Received Hello message after session established")
            is Welcome -> throw ProtocolViolationException("Receive Welcome message from client")
            is Abort -> session.connection.close("Abort from client")
            is Goodbye -> messageSender.sendGoodbye(session.connection)

            is Register -> remoteProcedureHandler.registerProcedure(session, message)
            is Unregister -> remoteProcedureHandler.unregisterProcedure(session, message)

            is Call -> remoteProcedureHandler.callProcedure(session, message)
            is Yield -> remoteProcedureHandler.handleYield(message)

            is Subscribe -> subscriptionHandler.subscribe(session, message)
            is Unsubscribe -> subscriptionHandler.unsubscribe(session, message)

            is Error -> handleError(message)

            else -> throw NotImplementedError("Message type ${message.messageType} not implemented")
        }
    }

    private fun handleError(errorMessage: Error) {
        when (errorMessage.requestType) {
            MessageType.INVOCATION -> remoteProcedureHandler.handleInvocationError(errorMessage)
            else -> throw NotImplementedError("Error with request type ${errorMessage.requestType} not implemented")
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

data class WampSession(val id: Long, val connection: Connection) {
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

    fun equals(other: WampSession): Boolean {
        return id == other.id
    }
}