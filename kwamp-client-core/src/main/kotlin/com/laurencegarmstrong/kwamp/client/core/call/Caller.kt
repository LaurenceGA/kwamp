package com.laurencegarmstrong.kwamp.client.core.call

import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

internal class Caller(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator
) {
    private val calls: ConcurrentHashMap<Long, CompletableDeferred<CallResult>> = ConcurrentHashMap()

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
            DeferredCallResult(CompletableDeferred<CallResult>().also {
                calls[requestId] = it
            })
        }


    fun result(resultMessage: Result) {
        calls.remove(resultMessage.requestId)?.complete(
            CallResult(
                resultMessage.arguments,
                resultMessage.argumentsKw
            )
        )
            ?: throw IllegalStateException("Could find request id ${resultMessage.requestId} in calls")
    }

    fun error(errorMessage: Error) {
        calls.remove(errorMessage.requestId)?.cancel(errorMessage.toCallException())
            ?: throw IllegalStateException("Could find request id ${errorMessage.requestId} in calls")
    }
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