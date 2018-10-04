package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.*
import co.nz.arm.kwamp.core.messages.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MessageSender {
    fun sendGoodbye(connection: Connection) = GlobalScope.launch {
        connection.send(Goodbye(mapOf(), WampClose.GOODBYE_AND_OUT.uri))
        connection.close("Closed by client.")
    }

    fun sendAbort(connection: Connection, exception: WampException) = GlobalScope.launch {
        connection.send(Abort(mapOf("message" to exception.localizedMessage), exception.error.uri))
        connection.close(exception.localizedMessage)
    }

    fun sendRegistered(connection: Connection, requestId: Long, registrationId: Long) = GlobalScope.launch {
        connection.send(Registered(requestId, registrationId))
    }

    fun sendUnregistered(connection: Connection, requestId: Long) = GlobalScope.launch {
        connection.send(Unregistered(requestId))
    }

    fun sendExceptionError(connection: Connection, wampError: WampErrorException) = GlobalScope.launch {
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
        GlobalScope.launch {
            procedureImplementingConnection.send(Invocation(requestId, registration, details, arguments, argumentsKw))
        }

    fun sendResult(
        callerConnection: Connection,
        callRequestId: Long,
        details: Dict,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) =
        GlobalScope.launch {
            callerConnection.send(Result(callRequestId, details, arguments, argumentsKw))
        }

    fun sendCallError(
        callerConnection: Connection,
        callRequestId: Long,
        details: Dict,
        error: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) =
        GlobalScope.launch {
            callerConnection.send(Error(MessageType.CALL, callRequestId, details, error, arguments, argumentsKw))
        }

    fun sendSubscribed(connection: Connection, requestId: Long, subscribeRequestId: Long) = GlobalScope.launch {
        connection.send(Subscribed(requestId, subscribeRequestId))
    }

    fun sendUnsubscribe(connection: Connection, unsubscribeRequestId: Long) = GlobalScope.launch {
        connection.send(Unsubscribed(unsubscribeRequestId))
    }
}