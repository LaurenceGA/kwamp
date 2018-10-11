package com.laurencegarmstrong.kwamp.router.core

import co.nz.arm.kwamp.core.*
import co.nz.arm.kwamp.core.messages.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class Realm(
    val uri: Uri
) {
    private val messageSender: MessageSender =
        MessageSender()
    private val dealer: Dealer =
        Dealer(
            messageSender,
            LinearIdGenerator(),
            RandomIdGenerator()
        )
    private val broker: Broker =
        Broker(messageSender, RandomIdGenerator())

    private val sessions = SessionSet(RandomIdGenerator())

    suspend fun join(connection: Connection) = startSession(connection)

    private suspend fun startSession(connection: Connection) = sessions.newSession(connection).apply {
        connection.forEachMessage {
            try {
                handleMessage(it, this)
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

    private suspend fun handleMessage(
        message: Message,
        session: WampSession
    ) {
        when (message) {
            is Hello -> throw ProtocolViolationException("Received Hello message after session established")
            is Welcome -> throw ProtocolViolationException("Receive Welcome message from client")
            is Abort -> session.connection.close("Abort from client")
            is Goodbye -> messageSender.sendGoodbye(session.connection)

            is Register -> dealer.registerProcedure(session, message)
            is Unregister -> dealer.unregisterProcedure(session, message)

            is Call -> dealer.callProcedure(session, message)
            is Yield -> dealer.handleYield(message)

            is Subscribe -> broker.subscribe(session, message)
            is Unsubscribe -> broker.unsubscribe(session, message)

            is Publish -> broker.publish(session, message)

            is Error -> handleError(message)

            //TODO protocol violation?
            else -> throw NotImplementedError("Message type ${message.messageType} not implemented")
        }
    }

    private fun handleError(errorMessage: Error) {
        when (errorMessage.requestType) {
            MessageType.INVOCATION -> dealer.handleInvocationError(errorMessage)
            //TODO protocol violation?
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
                    mapOf(
                        "agent" to "KWAMP",
                        "roles" to mapOf<String, Any?>(
                            "broker" to emptyMap<String, Any?>()
                            , "dealer" to emptyMap<String, Any?>()
                        )
                    )
                )
            )
        }
    }

    fun equals(other: WampSession): Boolean {
        return id == other.id
    }
}