package com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure

import com.laurencegarmstrong.kwamp.conversations.core.ConversationCanvas
import com.laurencegarmstrong.kwamp.conversations.core.STANDARD_TIMEOUT
import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Event
import com.laurencegarmstrong.kwamp.core.messages.Hello
import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.messages.Welcome
import com.laurencegarmstrong.kwamp.router.core.Router
import io.kotlintest.assertSoftly
import io.kotlintest.be
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.maps.shouldContainExactly
import io.kotlintest.should
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class RouterConversation(
    router: Router,
    vararg clients: TestConnection,
    conversationDefinition: RouterConversationCanvas.() -> Unit
) {
    init {
        runBlocking {
            clients.forEach { testConnection ->
                launch {
                    router.registerConnection(testConnection.connection)
                }
            }
        }
        RouterConversationCanvas().conversationDefinition()
    }
}

class RouterConversationCanvas : ConversationCanvas() {
    infix fun TestConnection.willSend(messageSupplier: () -> Message) {
        send(messageSupplier())
    }

    fun TestConnection.startsASession() {
        willSend { Hello(Uri("default"), emptyMap()) }
        shouldReceiveMessage<Welcome>()
    }

    inline infix fun <reified T : Message> TestConnection.shouldReceiveMessage(crossinline messageVerifier: (message: T) -> Unit) {
        runBlocking {
            withTimeout(STANDARD_TIMEOUT) {
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
            withTimeout(STANDARD_TIMEOUT) {
                val message = receive()
                message should beInstanceOf<T>()
            }
        }
    }

    internal fun eventMessageMatching(
        subscription: () -> Long,
        publication: () -> Long,
        arguments: List<Any?>,
        argumentsKw: Map<String, Any?>
    ) =
        { message: Event ->
            message.subscription should be(subscription())
            message.publication should be(publication())
            message.arguments!!.shouldContainExactly(*arguments.toTypedArray())
            message.argumentsKw!!.shouldContainExactly(argumentsKw)
        }
}