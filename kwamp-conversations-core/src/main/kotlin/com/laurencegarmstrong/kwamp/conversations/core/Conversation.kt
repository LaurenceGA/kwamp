package com.laurencegarmstrong.kwamp.conversations.core

import com.laurencegarmstrong.kwamp.client.core.Client
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Hello
import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.messages.Welcome
import com.laurencegarmstrong.kwamp.core.serialization.MessageSerializer
import com.laurencegarmstrong.kwamp.core.serialization.json.JsonMessageSerializer
import com.laurencegarmstrong.kwamp.router.core.Router
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

const val RECEIVE_TIMEOUT = 10000L

class RouterConversation(
    router: Router,
    vararg clients: TestConnection,
    conversationDefinition: ConversationCanvas.() -> Unit
) {
    init {
        runBlocking {
            clients.forEach { testConnection ->
                launch {
                    router.registerConnection(testConnection.connection)
                }
            }
        }
        ConversationCanvas().conversationDefinition()
    }
}

class ClientConversation(
    testRouter: TestConnection,
    realm: Uri = Uri("default"),
    conversationDefinition: ClientConversationCanvas.() -> Unit
) {
    init {
        ClientConversationCanvas(testRouter, realm).conversationDefinition()
    }
}

open class ConversationCanvas {
    infix fun TestConnection.willSend(messageSupplier: () -> Message) {
        runBlocking { send(messageSupplier()) }
    }

    fun TestConnection.startsASession() {
        willSend { Hello(Uri("default"), emptyMap()) }
        shouldReceiveMessage<Welcome>()
    }

    inline infix fun <reified T : Message> TestConnection.shouldReceiveMessage(crossinline messageVerifier: (message: T) -> Unit) {
        runBlocking {
            withTimeout(RECEIVE_TIMEOUT) {
                val message = receive()
                if (message !is T) {
                    message should beInstanceOf<T>()
                } else {
                    assertSoftly {
                        messageVerifier(message)
                    }
                }
            }
        }
    }

    inline fun <reified T : Message> TestConnection.shouldReceiveMessage() {
        runBlocking {
            withTimeout(RECEIVE_TIMEOUT) {
                val message = receive()
                message should beInstanceOf<T>()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun asDict(map: Any?) = map!! as HashMap<String, Any?>
}

class ClientConversationCanvas(
    val router: TestConnection,
    val realm: Uri
) : ConversationCanvas() {
    fun newTestClient() = Client(router.incoming, router.outgoing, realm)
}

class TestConnection(
    private val messageSerializer: MessageSerializer = JsonMessageSerializer(),
    channelCapacity: Int = 5
) {
    val incoming = Channel<ByteArray>(channelCapacity)
    val outgoing = Channel<ByteArray>(channelCapacity)
    val connection = Connection(incoming, outgoing, { }, messageSerializer)

    fun send(message: Message) {
        runBlocking {
            incoming.send(messageSerializer.serialize(message))
        }
    }

    suspend fun receive() = messageSerializer.deserialize(outgoing.receive())
}