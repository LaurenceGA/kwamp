package co.nz.arm.kwamp.conversations.infrastructure

import co.nz.arm.kwamp.core.Connection
import co.nz.arm.kwamp.core.messages.Message
import co.nz.arm.kwamp.core.serialization.JsonMessageSerializer
import co.nz.arm.kwamp.core.serialization.MessageSerializer
import co.nz.arm.kwamp.router.Router
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class Conversation(
    router: Router,
    vararg clients: TestConnection,
    conversationDefinition: ConversationCanvas.() -> Unit
) {
    private val canvas: ConversationCanvas = ConversationCanvas().apply(conversationDefinition)

    init {
        clients.forEach { testConnection ->
            GlobalScope.launch {
                router.registerConnection(testConnection.connection)
            }
        }
    }

    fun execute() = canvas.actions.forEach { action -> action() }
}

const val RECEIVE_TIMEOUT = 3000

class ConversationCanvas() {
    val actions: MutableList<() -> Unit> = mutableListOf()

    infix fun TestConnection.willSend(messageSupplier: () -> Message) {
        actions += { runBlocking { send(messageSupplier()) } }
    }

    inline infix fun <reified T : Message> TestConnection.shouldReceiveMessage(crossinline messageVerifier: (message: T) -> Unit) {
        actions += {
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
    }

    inline fun <reified T : Message> TestConnection.shouldReceiveMessage() {
        actions += {
            runBlocking {
                withTimeout(RECEIVE_TIMEOUT) {
                    val message = receive()
                    message should beInstanceOf<T>()
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun asDict(map: Any?) = map!! as HashMap<String, Any?>
}

class TestConnection(
    private val messageSerializer: MessageSerializer = JsonMessageSerializer(),
    channelCapacity: Int = 5
) {
    private val incoming = Channel<ByteArray>(channelCapacity)
    private val outgoing = Channel<ByteArray>(channelCapacity)
    val connection = Connection(incoming, outgoing, { }, messageSerializer)

    fun send(message: Message) {
        runBlocking {
            incoming.send(messageSerializer.serialize(message))
        }
    }

    fun receive() = runBlocking {
        messageSerializer.deserialize(outgoing.receive())
    }
}