package com.laurencegarmstrong.kwamp.conversations.core

import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.serialization.MessageSerializer
import com.laurencegarmstrong.kwamp.core.serialization.json.JsonMessageSerializer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

const val STANDARD_TIMEOUT = 10000L

open class ConversationCanvas {
    @Suppress("UNCHECKED_CAST")
    fun asDict(map: Any?) = map!! as HashMap<String, Any?>
}

class TestConnection(
    private val messageSerializer: MessageSerializer = JsonMessageSerializer(),
    channelCapacity: Int = 5
) {
    val incoming = Channel<ByteArray>(channelCapacity)
    val outgoing = Channel<ByteArray>(channelCapacity)
    val connection = Connection(incoming, outgoing, { incoming.close() }, messageSerializer)

    fun send(message: Message) {
        runBlocking {
            incoming.send(messageSerializer.serialize(message))
        }
    }

    suspend fun receive() = messageSerializer.deserialize(outgoing.receive())
}