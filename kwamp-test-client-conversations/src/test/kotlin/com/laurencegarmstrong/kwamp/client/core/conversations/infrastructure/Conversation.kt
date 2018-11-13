package com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure

import com.laurencegarmstrong.kwamp.client.core.Client
import com.laurencegarmstrong.kwamp.client.core.ClientImpl
import com.laurencegarmstrong.kwamp.client.core.call.CallHandler
import com.laurencegarmstrong.kwamp.client.core.pubsub.EventHandler
import com.laurencegarmstrong.kwamp.conversations.core.ConversationCanvas
import com.laurencegarmstrong.kwamp.conversations.core.RECEIVE_TIMEOUT
import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.UriPattern
import com.laurencegarmstrong.kwamp.core.messages.Dict
import com.laurencegarmstrong.kwamp.core.messages.Hello
import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.messages.Welcome
import io.kotlintest.assertSoftly
import io.kotlintest.be
import io.kotlintest.matchers.beInstanceOf
import io.kotlintest.should
import kotlinx.coroutines.*
import java.util.concurrent.Executors

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
) : ConversationCanvas(), CoroutineScope by CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher()) {
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

class TestClient : Client {
    private var client: ClientImpl? = null
    private val connection = TestConnection()

    internal fun connect(realm: Uri) {
        if (client != null) throw ClientAlreadyConnected()
        client = ClientImpl(connection.incoming, connection.outgoing, realm)
    }

    fun send(message: Message) = connection.send(message)

    suspend fun receive() = connection.receive()

    override fun register(procedure: Uri, handler: CallHandler) =
        client?.register(procedure, handler) ?: throw ClientNotConnected()

    override fun call(procedure: Uri, arguments: List<Any?>?, argumentsKw: Dict?) =
        client?.call(procedure, arguments, argumentsKw) ?: throw ClientNotConnected()

    override fun disconnect(closeReason: Uri) =
        client?.disconnect(closeReason) ?: throw ClientNotConnected()

    override fun publish(topic: Uri, arguments: List<Any?>?, argumentsKw: Dict?, onPublished: ((Long) -> Unit)?) =
        client?.publish(topic, arguments, argumentsKw, onPublished) ?: throw ClientNotConnected()


    override fun subscribe(topicPattern: UriPattern, eventHandler: EventHandler) =
        client?.subscribe(topicPattern, eventHandler) ?: throw ClientNotConnected()

}

class ClientNotConnected : IllegalStateException()
class ClientAlreadyConnected : IllegalStateException()