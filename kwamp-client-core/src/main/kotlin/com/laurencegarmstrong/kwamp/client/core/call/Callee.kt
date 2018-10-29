package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.client.core.MessageListenersHandler
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.ProtocolViolationException
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

internal class Callee(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator,
    private val messageListenersHandler: MessageListenersHandler
) {
    private val registrations = ConcurrentHashMap<Long, CallHandler>()

    fun register(
        procedure: Uri,
        handler: CallHandler
    ): RegistrationHandle =
        runBlocking {
            val registered = createRegistration(procedure)
            registrations[registered.registration] = handler
            RegistrationHandle { unregister(registered.registration) }
        }

    private suspend fun createRegistration(procedure: Uri): Registered {
        randomIdGenerator.newId().also { requestId ->
            connection.send(
                Register(
                    requestId,
                    emptyMap(),
                    procedure
                )
            )
            return deferredRegisteredWithListeners(requestId).await()
        }
    }

    private fun deferredRegisteredWithListeners(requestId: Long): Deferred<Registered> =
        CompletableDeferred<Registered>().also {
            applyListenersToCompletableRegistered(it, requestId)
        }

    private fun applyListenersToCompletableRegistered(
        completableResult: CompletableDeferred<Registered>,
        requestId: Long
    ) = GlobalScope.launch {
        val deferredRegisteredMessage = messageListenersHandler.registerListener<Registered>(requestId)
        val deferredErrorMessage = messageListenersHandler.registerListener<Error>(requestId)
        launch {
            val registeredMessage = deferredRegisteredMessage.await()
            deferredErrorMessage.cancel()
            completableResult.complete(registeredMessage)
        }
        launch {
            val errorMessage = deferredErrorMessage.await()
            deferredRegisteredMessage.cancel()
            completableResult.cancel(errorMessage.toCallException())
        }
    }

    private fun unregister(registrationId: Long) {
        runBlocking {
            //TODO consider current invocations
            performUnregister(registrationId)
            registrations.remove(registrationId)
        }

    }

    private suspend fun performUnregister(registrationId: Long) {
        randomIdGenerator.newId().also { requestId ->
            connection.send(
                Unregister(
                    requestId,
                    registrationId
                )
            )
            deferredUnregisteredWithListeners(requestId).await()
        }
    }

    private fun deferredUnregisteredWithListeners(requestId: Long): Deferred<Unregistered> =
        CompletableDeferred<Unregistered>().also {
            applyListenersToCompletableUnregistered(it, requestId)
        }

    private fun applyListenersToCompletableUnregistered(
        completableResult: CompletableDeferred<Unregistered>,
        requestId: Long
    ) = GlobalScope.launch {
        val deferredUnregisteredMessage = messageListenersHandler.registerListener<Unregistered>(requestId)
        val deferredErrorMessage = messageListenersHandler.registerListener<Error>(requestId)
        launch {
            val registeredMessage = deferredUnregisteredMessage.await()
            deferredErrorMessage.cancel()
            completableResult.complete(registeredMessage)
        }
        launch {
            val errorMessage = deferredErrorMessage.await()
            deferredUnregisteredMessage.cancel()
            completableResult.cancel(errorMessage.toCallException())
        }
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