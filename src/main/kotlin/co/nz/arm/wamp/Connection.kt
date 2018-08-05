package co.nz.arm.wamp

import co.nz.arm.wamp.messages.Message
import co.nz.arm.wamp.messages.MessageType
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch

class Connection(private val incoming: ReceiveChannel<String>, private val outgoing: SendChannel<String>, private val closeConnection: suspend (message: String) -> Unit) {
    suspend fun close(message: String) {
        outgoing.close()
        closeConnection(message)
    }

    suspend fun forEachMessage(action: suspend (Message) -> Unit) = launch {
        incoming.consumeEach { processRawMessage(it, action) }
    }

    suspend fun onNextMessage(action: suspend (Message) -> Unit) = launch {
        processRawMessage(incoming.receive(), action)
    }

    private suspend fun processRawMessage(message: String, action: suspend (Message) -> Unit) {
        deserialize(message).also { action(it) }
    }

    private fun deserialize(rawMessage: String): Message {
        val messageArray = Klaxon().parseArray<Any>(rawMessage)
        return MessageType.getFactory(messageArray!![0] as Int)?.invoke(messageArray.subList(1, messageArray.size))!!
    }

    private fun serialize(message: Message) = Klaxon().toJsonString(message.toList())

    suspend fun send(message: Message) {
        outgoing.send(serialize(message))
    }
}