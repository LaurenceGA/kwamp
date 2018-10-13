package com.laurencegarmstrong.kwamp.core

import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.serialization.MessageSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory

class Connection(
    private val incoming: ReceiveChannel<ByteArray>,
    private val outgoing: SendChannel<ByteArray>,
    private val closeConnection: suspend (message: String) -> Unit,
    private val messageSerializer: MessageSerializer
) : MessageSerializer by messageSerializer {
    private val logger = LoggerFactory.getLogger(Connection::class.java)!!

    suspend fun close(message: String) {
        while (outgoing.isFull) {
            // Wait until messages are sent through the channel
        }
        closeConnection(message)
        outgoing.close()
    }

    suspend fun forEachMessage(action: suspend (Message) -> Unit) =
        GlobalScope.launch(context = cancelOnException) {
            incoming.consumeEach { processRawMessage(it, action) }
        }

    suspend fun <R> onNextMessage(action: suspend (Message) -> R): Deferred<R> =
        GlobalScope.async(context = cancelOnException) {
            processRawMessage(incoming.receive(), action)
        }

    suspend inline fun <reified T : Message, R> withNextMessage(crossinline action: suspend (message: T) -> R): Deferred<R> {
        val job = onNextMessage {
            when (it) {
                is T -> action(it)
                else -> throw UnexpectedMessageException(T::class, it::class)
            }
        }
        job.invokeOnCompletion { throwable ->
            if (throwable != null) throw throwable
        }
        return job
    }

    private suspend fun <R> processRawMessage(message: ByteArray, action: suspend (Message) -> R): R =
        action(deserialize(message).also { logger.info("Received $it") })

    suspend fun send(message: Message) {
        logger.info("Sending $message")
        outgoing.send(serialize(message))
    }
}

val cancelOnException = Dispatchers.Default + CoroutineExceptionHandler { context, throwable ->
    context.cancel(throwable)
}