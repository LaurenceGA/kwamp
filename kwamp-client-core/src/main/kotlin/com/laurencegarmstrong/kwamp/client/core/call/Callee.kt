package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.client.core.MessageListenersHandler
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.WampError
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

internal class Callee(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator,
    private val messageListenersHandler: MessageListenersHandler
) : CoroutineScope by CoroutineScope(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()) {
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
            return messageListenersHandler.registerListenerWithErrorHandler<Registered>(requestId).await()
        }
    }

    private fun unregister(registrationId: Long) {
        runBlocking {
            registrations.remove(registrationId)
            unregisterWithRouter(registrationId)
        }

    }

    private suspend fun unregisterWithRouter(registrationId: Long) {
        randomIdGenerator.newId().also { requestId ->
            connection.send(
                Unregister(
                    requestId,
                    registrationId
                )
            )
            messageListenersHandler.registerListenerWithErrorHandler<Unregistered>(requestId).await()
        }
    }

    fun invokeProcedure(invocationMessage: Invocation) {
        val registeredFunction = registrations[invocationMessage.registration]

        if (registeredFunction == null) {
            launch {
                val errorMessage =
                    "INVOCATION received for non-registered registration ID (${invocationMessage.registration}) requestID=${invocationMessage.requestId}"
                connection.send(
                    Abort(
                        mapOf("message" to errorMessage),
                        WampError.PROTOCOL_VIOLATION.uri
                    )
                )
                connection.close(errorMessage)
            }
            return
        }

        try {
            val result = registeredFunction.invoke(
                invocationMessage.arguments,
                invocationMessage.argumentsKw
            )

            //Verify function is still registered in case it was unregistered during processing
            if (!registrations.containsKey(invocationMessage.registration)) {
                throw IllegalStateException("Procedure has been unregistered during processing")
            }

            launch {
                connection.send(
                    Yield(
                        invocationMessage.requestId,
                        emptyMap(),
                        result.arguments,
                        result.argumentsKw
                    )
                )
            }
        } catch (error: CallException) {
            launch {
                connection.send(
                    Error(
                        MessageType.INVOCATION,
                        invocationMessage.requestId,
                        emptyMap(),
                        error.error,
                        error.arguments,
                        error.argumentsKw
                    )
                )
            }
        } catch (error: Exception) {
            launch {
                connection.send(
                    Error(
                        MessageType.INVOCATION,
                        invocationMessage.requestId,
                        emptyMap(),
                        DEFAULT_INVOCATION_ERROR
                    )
                )
            }
        }
    }
}

typealias CallHandler = (arguments: List<Any?>?, argumentsKw: Dict?) -> CallResult

class RegistrationHandle(private val unregisterCallback: () -> Unit) {
    fun unregister() = unregisterCallback()
}