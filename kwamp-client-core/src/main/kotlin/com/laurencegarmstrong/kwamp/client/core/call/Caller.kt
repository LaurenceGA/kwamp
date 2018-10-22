package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.client.core.MessageListenersHandler
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Call
import com.laurencegarmstrong.kwamp.core.messages.Dict
import com.laurencegarmstrong.kwamp.core.messages.Error
import com.laurencegarmstrong.kwamp.core.messages.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

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


    //TODO messageSender?
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

        applyListenersToCompletableResult(completableResult, requestId)

        return DeferredCallResult(completableResult)
    }

    private fun applyListenersToCompletableResult(
        completableResult: CompletableDeferred<CallResult>,
        requestId: Long
    ) = GlobalScope.launch {
        val deferredResultMessage = messageListenersHandler.registerListener<Result>(requestId)
        val deferredErrorMessage = messageListenersHandler.registerListener<Error>(requestId)
        launch {
            val resultMessage = deferredResultMessage.await()
            deferredErrorMessage.cancel()
            completableResult.complete(resultMessageToCallResult(resultMessage))
        }
        launch {
            val errorMessage = deferredErrorMessage.await()
            deferredResultMessage.cancel()
            completableResult.cancel(errorMessage.toCallException())
        }
    }

    private fun resultMessageToCallResult(resultMessage: Result) = CallResult(
        resultMessage.arguments,
        resultMessage.argumentsKw
    )
}

class DeferredCallResult(private val deferredResult: Deferred<CallResult>) {
    suspend fun await() = deferredResult.await()
    suspend fun join() = deferredResult.join()

    fun invokeOnError(errorCallback: (CallError) -> Unit) {
        deferredResult.invokeOnCompletion { throwable ->
            (throwable as? CallError)?.apply { callbackAsynchronously(this, errorCallback) }
        }
    }

    private fun callbackAsynchronously(error: CallError, callback: (CallError) -> Unit) =
        GlobalScope.launch {
            callback(error)
        }
}