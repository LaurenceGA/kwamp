package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.client.core.MessageListenersHandler
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.WampErrorException
import com.laurencegarmstrong.kwamp.core.messages.Call
import com.laurencegarmstrong.kwamp.core.messages.Dict
import com.laurencegarmstrong.kwamp.core.messages.Result
import kotlinx.coroutines.*
import java.util.concurrent.Executors

internal class Caller(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator,
    private val messageListenersHandler: MessageListenersHandler
) {
    private val callSendScope =
        CoroutineScope(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher())
    private val receiveScope =
        CoroutineScope(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher())

    fun call(
        procedure: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ): DeferredCallResult = sendCallMessage(procedure, arguments, argumentsKw)

    private fun sendCallMessage(
        procedure: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) =
        randomIdGenerator.newId().let { requestId ->
            val resultListener = deferredResultWithListeners(requestId)

            callSendScope.launch {
                connection.send(
                    Call(
                        requestId,
                        emptyMap(),
                        procedure,
                        arguments,
                        argumentsKw
                    )
                )
            }

            resultListener
        }

    private fun deferredResultWithListeners(requestId: Long): DeferredCallResult {
        val completableResult = CompletableDeferred<CallResult>()

        receiveScope.launch {
            try {
                val resultMessage = messageListenersHandler.registerListenerWithErrorHandler<Result>(requestId).await()
                completableResult.complete(resultMessage.toCallResult())
            } catch (error: WampErrorException) {
                completableResult.completeExceptionally(error)
            }
        }

        return DeferredCallResult(completableResult)
    }
}

fun Result.toCallResult() = CallResult(arguments, argumentsKw)

class DeferredCallResult(private val deferredResult: Deferred<CallResult>) :
    CoroutineScope by CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    suspend fun await() = deferredResult.await()
    suspend fun join() = deferredResult.join()

    fun invokeOnError(errorCallback: (WampErrorException) -> Unit) {
        deferredResult.invokeOnCompletion { throwable ->
            (throwable as? WampErrorException)?.apply { errorCallback(this) }
        }
    }

    fun invokeOnSuccess(completionCallback: (CallResult) -> Unit) {
        deferredResult.invokeOnCompletion { throwable ->
            if (throwable == null) completionCallback(deferredResult.getCompleted())
        }
    }
}