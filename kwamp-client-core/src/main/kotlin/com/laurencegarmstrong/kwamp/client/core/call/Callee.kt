package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.ProtocolViolationException
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

internal class Callee(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator
) {
    private val pendingRegistrations = ConcurrentHashMap<Long, CompletableDeferred<Registered>>()
    private val registrations = ConcurrentHashMap<Long, CallHandler>()

    fun register(
        procedure: Uri,
        handler: CallHandler
    ): RegistrationHandle =
        runBlocking {
            val registered = createRegistration(randomIdGenerator.newId(), procedure)
            registrations[registered.registration] = handler
            RegistrationHandle { unregister(registered.registration) }
        }

    private suspend fun createRegistration(requestId: Long, procedure: Uri): Registered {
        sendRegister(requestId, procedure)
        return newPendingRegistration(requestId).await()  //TODO timeout maybe?
    }

    //TODO use correct exception
    fun receiveRegistered(registered: Registered) = pendingRegistrations[registered.requestId]?.complete(registered)
        ?: throw ProtocolViolationException("No register request with id ${registered.requestId}")

    private suspend fun sendRegister(requestId: Long, procedure: Uri) {
        connection.send(
            Register(
                requestId,
                emptyMap(),
                procedure
            )
        )
    }

    private fun newPendingRegistration(requestId: Long) =
        CompletableDeferred<Registered>().also { deferredRegistered ->
            pendingRegistrations[requestId] = deferredRegistered
            deferredRegistered.invokeOnCompletion {
                pendingRegistrations.remove(requestId)
            }
        }

    private fun unregister(registrationId: Long) {
        //TODO implement
    }

    fun invokeProcedure(invocationMessage: Invocation) {
        //TODO use correct exception
        val result = registrations[invocationMessage.registration]?.invoke(
            invocationMessage.arguments,
            invocationMessage.argumentsKw
        ) ?: throw ProtocolViolationException("No such registration ${invocationMessage.registration}")
        GlobalScope.launch {
            connection.send(
                Yield(
                    invocationMessage.requestId,
                    emptyMap(),
                    result.arguments,
                    result.argumentsKw
                )
            )
        }
    }
}

typealias CallHandler = (arguments: List<Any?>?, argumentsKw: Dict?) -> CallResult


class RegistrationHandle(private val unregisterCallback: () -> Unit) {
    fun unregister() = unregisterCallback()
}