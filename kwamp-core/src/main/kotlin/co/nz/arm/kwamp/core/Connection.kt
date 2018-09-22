package co.nz.arm.kwamp.core

import co.nz.arm.kwamp.core.messages.Message
import co.nz.arm.kwamp.core.serialization.MessageSerializer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach

class Connection(
    private val incoming: ReceiveChannel<ByteArray>,
    private val outgoing: SendChannel<ByteArray>,
    private val closeConnection: suspend (message: String) -> Unit,
    private val messageSerializer: MessageSerializer
) : MessageSerializer by messageSerializer {
    suspend fun close(message: String) {
        outgoing.close()
        closeConnection(message)
    }

    suspend fun forEachMessage(action: suspend (Message) -> Unit) =
        GlobalScope.launch(context = cancelOnException) {
            incoming.consumeEach { processRawMessage(it, action) }
        }

    suspend fun onNextMessage(action: suspend (Message) -> Unit) = GlobalScope.launch(context = cancelOnException) {
        processRawMessage(incoming.receive(), action)
    }

    private suspend fun processRawMessage(message: ByteArray, action: suspend (Message) -> Unit) {
        deserialize(message).also { action(it) }
    }

    suspend fun send(message: Message) {
        outgoing.send(serialize(message))
    }
}

val cancelOnException = Dispatchers.Default + CoroutineExceptionHandler { context, throwable ->
    context.cancel(throwable)
}