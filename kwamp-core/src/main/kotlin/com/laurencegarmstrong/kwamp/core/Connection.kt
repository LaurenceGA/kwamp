package com.laurencegarmstrong.kwamp.core

import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.serialization.MessageSerializer
import com.laurencegarmstrong.kwamp.core.serialization.json.JsonMessageSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

class Connection(
    private val incoming: ReceiveChannel<ByteArray>,
    private val outgoing: SendChannel<ByteArray>,
    private val closeConnection: suspend (message: String) -> Unit,
    private val messageSerializer: MessageSerializer = JsonMessageSerializer()
) : MessageSerializer by messageSerializer,
    CoroutineScope by CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + cancelOnException) {
    private val logger = LoggerFactory.getLogger(Connection::class.java)!!
    private var strictUris: Boolean = false

    suspend fun close(message: String) {
        closeConnection(message)
        outgoing.close()
    }

    fun forEachMessage(exceptionHandler: (Throwable) -> Unit, action: suspend (Message) -> Unit) =
        launch {
            incoming.consumeEach {
                try {
                    processRawMessage(it, action)
                } catch (throwable: Throwable) {
                    exceptionHandler(throwable)
                }
            }
        }

    fun <R> onNextMessage(action: suspend (Message) -> R): Deferred<R> =
        async {
            processRawMessage(incoming.receive(), action)
        }

    suspend inline fun <reified T : Message, R> withNextMessage(crossinline action: suspend (message: T) -> R) =
        onNextMessage {
            when (it) {
                is T -> action(it)
                else -> throw UnexpectedMessageException(T::class, it::class)
            }
        }

    private suspend fun <R> processRawMessage(message: ByteArray, action: suspend (Message) -> R): R =
        action(deserialize(message).apply {
            validateUris(strictUris)
            logger.info("Received $this")
        })

    suspend fun send(message: Message) {
        logger.info("Sending $message")
        outgoing.send(serialize(message))
    }

    fun setStrictUris(strictUris: Boolean) {
        this.strictUris = strictUris
    }
}

val cancelOnException = CoroutineExceptionHandler { context, throwable ->
    context.cancel(CancellationException("Coroutine cancelled", throwable))
}