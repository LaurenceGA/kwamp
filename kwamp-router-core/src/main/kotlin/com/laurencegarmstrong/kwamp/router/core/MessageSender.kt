package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MessageSender : CoroutineScope by CoroutineScope(Executors.newFixedThreadPool(4).asCoroutineDispatcher()) {

    fun sendGoodbye(connection: Connection) = launch {
        connection.send(
            Goodbye(
                mapOf(),
                WampClose.GOODBYE_AND_OUT.uri
            )
        )
        connection.close("Closed by client.")
    }

    fun sendAbort(connection: Connection, exception: WampException) = launch {
        connection.send(
            Abort(
                mapOf("message" to exception.localizedMessage),
                exception.error
            )
        )
        connection.close(exception.localizedMessage)
    }

    fun sendWelcome(connection: Connection, sessionId: Long, details: Dict) = launch {
        connection.send(Welcome(sessionId, details))
    }

    fun sendRegistered(connection: Connection, requestId: Long, registrationId: Long) = launch {
        connection.send(Registered(requestId, registrationId))
    }

    fun sendUnregistered(connection: Connection, requestId: Long) = launch {
        connection.send(Unregistered(requestId))
    }

    fun sendExceptionError(connection: Connection, wampError: WampErrorException) = launch {
        connection.send(wampError.getErrorMessage())
    }

    fun sendInvocation(
        procedureImplementingConnection: Connection,
        requestId: Long,
        registration: Long,
        details: Dict,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) =
        launch {
            procedureImplementingConnection.send(
                Invocation(
                    requestId,
                    registration,
                    details,
                    arguments,
                    argumentsKw
                )
            )
        }

    fun sendResult(
        callerConnection: Connection,
        callRequestId: Long,
        details: Dict,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) =
        launch {
            callerConnection.send(
                Result(
                    callRequestId,
                    details,
                    arguments,
                    argumentsKw
                )
            )
        }

    fun sendCallError(
        callerConnection: Connection,
        callRequestId: Long,
        details: Dict,
        error: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) =
        launch {
            callerConnection.send(
                Error(
                    MessageType.CALL,
                    callRequestId,
                    details,
                    error,
                    arguments,
                    argumentsKw
                )
            )
        }

    fun sendSubscribed(connection: Connection, requestId: Long, subscribeRequestId: Long) = launch {
        connection.send(Subscribed(requestId, subscribeRequestId))
    }

    fun sendUnsubscribe(connection: Connection, unsubscribeRequestId: Long) = launch {
        connection.send(Unsubscribed(unsubscribeRequestId))
    }

    fun sendPublished(connection: Connection, requestId: Long, publicationId: Long) = launch {
        connection.send(Published(requestId, publicationId))
    }

    fun sendEvent(
        connection: Connection,
        subscriptionId: Long,
        publicationId: Long,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) = launch {
        connection.send(
            Event(
                subscriptionId,
                publicationId,
                emptyMap(),
                arguments,
                argumentsKw
            )
        )
    }
}