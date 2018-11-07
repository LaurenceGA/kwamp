package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Realm(
    val uri: Uri
) {
    private val messageSender: MessageSender = MessageSender()
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

    private suspend fun startSession(connection: Connection) = sessions.newSession(connection).also { session ->
        GlobalScope.launch {
            session.listenForMessages()
        }
    }

    private suspend fun WampSession.listenForMessages() {
        connection.forEachMessage(exceptionHandler(connection)) {
            handleMessage(it, this)
        }.invokeOnCompletion { fatalException ->
            when (fatalException) {
                is ProtocolViolationException -> messageSender.sendAbort(connection, fatalException)
                else -> fatalException?.run { printStackTrace() }
            }
            sessions.endSession(id)
        }
    }

    private fun exceptionHandler(connection: Connection): (Throwable) -> Unit = { throwable ->
        when (throwable) {
            is WampErrorException -> messageSender.sendExceptionError(connection, throwable)
            else -> throw throwable
        }
    }

    private suspend fun handleMessage(
        message: Message,
        session: WampSession
    ) {
        when (message) {
            is Hello -> throw ProtocolViolationException(
                "Received Hello message after session established"
            )
            is Welcome -> throw ProtocolViolationException(
                "Receive Welcome message from client"
            )
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

class SessionSet(idGenerator: WampIdGenerator) {
    private val sessions = IdentifiableSet<WampSession>(idGenerator)

    fun newSession(connection: Connection) = sessions.putWithId { id ->
        WampSession(id, connection)
    }

    fun endSession(id: Long) = sessions.remove(id)
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