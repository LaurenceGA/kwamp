package com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure

import com.laurencegarmstrong.kwamp.conversations.core.ConversationCanvas
import com.laurencegarmstrong.kwamp.conversations.core.RECEIVE_TIMEOUT
import com.laurencegarmstrong.kwamp.conversations.core.TestClient
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Hello
import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.messages.Welcome
import io.kotlintest.assertSoftly
import io.kotlintest.be
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import kotlinx.coroutines.*

class ClientConversation(
    realm: Uri = Uri("default"),
    conversationDefinition: ClientConversationCanvas.() -> Unit
) {
    init {
        ClientConversationCanvas(realm).conversationDefinition()
    }
}

class ClientConversationCanvas(
    private val realm: Uri
) : ConversationCanvas(), CoroutineScope by GlobalScope {
    infix fun TestClient.willBeSentRouterMessage(messageSupplier: () -> Message) {
        send(messageSupplier())
    }

    fun launchWithTimeout(timeout: Long = RECEIVE_TIMEOUT, block: suspend CoroutineScope.() -> Unit) =
        launch {
            withTimeout(timeout, block)
        }

    fun <T> asyncWithTimeout(timeout: Long = RECEIVE_TIMEOUT, block: suspend CoroutineScope.() -> T) =
        async {
            withTimeout(timeout, block)
        }

    fun <T> runBlockingWithTimeout(timeout: Long = RECEIVE_TIMEOUT, block: suspend CoroutineScope.() -> T) =
        runBlocking {
            withTimeout(timeout, block)
        }

    inline infix fun <reified T : Message> TestClient.shouldHaveSentMessage(crossinline messageVerifier: (message: T) -> Unit) {
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

    fun newConnectedTestClient(sessionId: Long = 123L): TestClient {
        val client = TestClient()
        val connectionJob = launchWithTimeout {
            client.connect()
        }
        client shouldHaveSentMessage { message: Hello ->
            message.realm should be(realm)
        }
        client willBeSentRouterMessage { Welcome(sessionId, emptyMap()) }

        runBlocking {
            connectionJob.join()
        }

        return client
    }

    fun TestClient.connect(testRealm: Uri = realm) {
        this.connect(testRealm)
    }
}