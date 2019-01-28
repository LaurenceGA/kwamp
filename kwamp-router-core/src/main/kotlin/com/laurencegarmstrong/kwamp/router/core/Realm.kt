package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ForkJoinPool
import kotlin.coroutines.CoroutineContext

internal val WELCOME_MESSAGE = mapOf(
    "agent" to "KWAMP",
    "roles" to mapOf<String, Any?>(
        "broker" to emptyMap<String, Any?>(),
        "dealer" to emptyMap<String, Any?>()
    )
)

class Realm(
    val uri: Uri,
    private val messageSender: MessageSender
) : CoroutineScope {
    private val dealer: Dealer =
        Dealer(
            messageSender,
            LinearIdGenerator(),
            RandomIdGenerator()
        )
    private val broker: Broker =
        Broker(messageSender, RandomIdGenerator())

    private val sessions = SessionSet(RandomIdGenerator())

    private val sessionThreadPool = ForkJoinPool().asCoroutineDispatcher()
    override val coroutineContext: CoroutineContext
        get() = sessionThreadPool + CoroutineName("Realm session thread pool")

    fun join(connection: Connection) = startSession(connection)

    private fun startSession(connection: Connection) = sessions.newSession(connection).also { session ->
        launch {
            session.sendWelcomeMessage()
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
            sessions.endSession(id)?.also {
                dealer.cleanSessionResources(id)
                broker.cleanSessionResources(id)
            }
        }
    }

    private fun WampSession.sendWelcomeMessage() = messageSender.sendWelcome(connection, id, WELCOME_MESSAGE)

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

            else -> throw ProtocolViolationException("Message type ${message.messageType} is not a valid type")
        }
    }

    private fun exceptionHandler(connection: Connection): (Throwable) -> Unit = { throwable ->
        when (throwable) {
            is WampErrorException -> messageSender.sendExceptionError(connection, throwable)
            else -> throw throwable
        }
    }

    private fun handleError(errorMessage: Error) {
        when (errorMessage.requestType) {
            MessageType.INVOCATION -> dealer.handleInvocationError(errorMessage)
            else -> throw ProtocolViolationException("Error with request type ${errorMessage.requestType} - not valid type")
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
    fun equals(other: WampSession): Boolean {
        return id == other.id
    }
}