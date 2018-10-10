package nz.co.arm.kwamp.client

import co.nz.arm.kwamp.core.Connection
import co.nz.arm.kwamp.core.RandomIdGenerator
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.messages.Call
import co.nz.arm.kwamp.core.messages.Dict
import co.nz.arm.kwamp.core.messages.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class Caller(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator
) {
    private val calls: ConcurrentHashMap<Long, CompletableDeferred<CallResult>> = ConcurrentHashMap()

    fun call(
        procedure: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ): Deferred<CallResult> = sendCallMessage(procedure, arguments, argumentsKw)


    //TODO messageSender?
    private fun sendCallMessage(
        procedure: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) =
        randomIdGenerator.newId().let { requestId ->
            GlobalScope.launch {
                connection.send(Call(requestId, emptyMap(), procedure, arguments, argumentsKw))
            }
            CompletableDeferred<CallResult>().also {
                calls[requestId] = it
            }
        }


    fun result(resultMessage: Result) {
        calls[resultMessage.requestId]?.complete(CallResult(resultMessage.arguments, resultMessage.argumentsKw))
    }
}

data class CallResult(val arguments: List<Any?>? = null, val argumentsKw: Dict? = null)