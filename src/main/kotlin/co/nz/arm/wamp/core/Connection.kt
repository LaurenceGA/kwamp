package co.nz.arm.wamp.core

import co.nz.arm.wamp.core.messages.Message
import co.nz.arm.wamp.core.serialization.MessageSerializer
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.consumeEach

class Connection(private val incoming: ReceiveChannel<ByteArray>,
                 private val outgoing: SendChannel<ByteArray>,
                 private val closeConnection: suspend (message: String) -> Unit,
                 private val messageSerializer: MessageSerializer) : MessageSerializer by messageSerializer {
    suspend fun close(message: String) {
        outgoing.close()
        closeConnection(message)
    }

    suspend fun forEachMessage(action: suspend (Message) -> Unit) = launch(cancelOnException) {
        incoming.consumeEach { processRawMessage(it, action) }
    }

    suspend fun onNextMessage(action: suspend (Message) -> Unit) = launch(cancelOnException) {
        processRawMessage(incoming.receive(), action)
    }

    private suspend fun processRawMessage(message: ByteArray, action: suspend (Message) -> Unit) {
        deserialize(message).also { action(it) }
    }

    suspend fun send(message: Message) {
        outgoing.send(serialize(message))
    }
}

val cancelOnException = DefaultDispatcher + CoroutineExceptionHandler { context, throwable ->
    context.cancel(throwable)
}