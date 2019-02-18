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
            GlobalScope.launch {
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

            deferredResultWithListeners(requestId)
        }

    private fun deferredResultWithListeners(requestId: Long): DeferredCallResult {
        val completableResult = CompletableDeferred<CallResult>()

        GlobalScope.launch {
            try {
                val resultMessage = messageListenersHandler.registerListenerWithErrorHandler<Result>(requestId).await()
                completableResult.complete(resultMessageToCallResult(resultMessage))
            } catch (error: WampErrorException) {
                completableResult.completeExceptionally(error)
            }
        }

        return DeferredCallResult(completableResult)
    }

    private fun resultMessageToCallResult(resultMessage: Result) = CallResult(
        resultMessage.arguments,
        resultMessage.argumentsKw
    )
}

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