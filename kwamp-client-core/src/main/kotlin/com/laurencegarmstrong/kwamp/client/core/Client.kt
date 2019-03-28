package com.laurencegarmstrong.kwamp.client.core

import com.laurencegarmstrong.kwamp.client.core.call.*
import com.laurencegarmstrong.kwamp.client.core.pubsub.EventHandler
import com.laurencegarmstrong.kwamp.client.core.pubsub.Publisher
import com.laurencegarmstrong.kwamp.client.core.pubsub.Subscriber
import com.laurencegarmstrong.kwamp.client.core.pubsub.SubscriptionHandle
import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.*
import com.laurencegarmstrong.kwamp.core.serialization.json.JsonMessageSerializer
import com.laurencegarmstrong.kwamp.core.serialization.messagepack.MessagePackSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

interface Client {
    fun register(procedure: Uri, handler: CallHandler): RegistrationHandle

    fun call(
        procedure: Uri,
        arguments: List<Any?>? = null,
        argumentsKw: Dict? = null
    ): DeferredCallResult

    fun disconnect(closeReason: Uri = WampClose.SYSTEM_SHUTDOWN.uri): Uri

    fun publish(
        topic: Uri,
        arguments: List<Any?>? = null,
        argumentsKw: Dict? = null,
        onPublished: ((Long) -> Unit)? = null
    )

    fun subscribe(topicPattern: UriPattern, eventHandler: EventHandler): SubscriptionHandle
}

class ClientImpl(
    incoming: ReceiveChannel<ByteArray>,
    outgoing: SendChannel<ByteArray>,
    realm: Uri,
    protocol: String = WAMP_DEFAULT,
    onClose: suspend (message: String) -> Unit = {},
    private val exceptionCatcher: ExceptionCatcher = ExceptionSwallower()
) : Client, CoroutineScope by CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
    private val log = LoggerFactory.getLogger(ClientImpl::class.java)!!
    private val connection = Connection(incoming, outgoing, onClose, getSerializer(protocol))

    private var sessionId: Long? = null

    private val randomIdGenerator = RandomIdGenerator()

    private val messageListenersHandler = MessageListenersHandler()

    private val caller = Caller(connection, randomIdGenerator, messageListenersHandler)
    private val callee = Callee(connection, randomIdGenerator, messageListenersHandler)

    private val publisher = Publisher(connection, randomIdGenerator, messageListenersHandler)
    private val subscriber = Subscriber(connection, randomIdGenerator, messageListenersHandler)

    init {
        joinRealm(realm)

        launch {
            connection.forEachMessage(exceptionHandler()) {
                try {
                    handleMessage(it)
                } catch (nonFatalError: WampErrorException) {
                    exceptionCatcher.catchException(nonFatalError)
                }
            }.invokeOnCompletion { fatalException ->
                fatalException?.run { printStackTrace() }
            }
        }
    }

    private fun exceptionHandler(): (Throwable) -> Unit = { throwable ->
        when (throwable) {
            is WampErrorException -> exceptionCatcher.catchException(throwable)
            else -> throw throwable
        }
    }

    private fun handleMessage(message: Message) {
        messageListenersHandler.notifyListeners(message)

        when (message) {
            is Invocation -> callee.invokeProcedure(message)
            is Event -> subscriber.receiveEvent(message)

            is Error -> exceptionCatcher.catchException(message.toWampErrorException())
        }
    }

    private fun getSerializer(protocol: String) =
        when (protocol) {
            WAMP_JSON -> JsonMessageSerializer()
            WAMP_MSG_PACK -> MessagePackSerializer()
            else -> throw IllegalArgumentException("Unsupported sub protocol '$protocol'")
        }

    private fun joinRealm(realmUri: Uri) = runBlocking {
        connection.send(
            Hello(
                realmUri, mapOf(
                    "roles" to mapOf<String, Any?>(
                        "publisher" to emptyMap<String, Any?>(),
                        "subscriber" to emptyMap<String, Any?>(),
                        "caller" to emptyMap<String, Any?>(),
                        "callee" to emptyMap<String, Any?>()
                    )
                )
            )
        )
        connection.withNextMessage { message: Welcome ->
            log.info("Session established. ID: ${message.session}")
            sessionId = message.session
        }.join()
    }

    override fun register(procedure: Uri, handler: CallHandler) =
        callee.register(procedure, handler)

    override fun call(
        procedure: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) = caller.call(procedure, arguments, argumentsKw)

    override fun disconnect(closeReason: Uri) = runBlocking {
        val messageListener = messageListenersHandler.registerListener<Goodbye>()

        connection.send(Goodbye(emptyMap(), closeReason))

        messageListener.await().let { message ->
            log.info("Router replied goodbye. Reason: ${message.reason}")
            message.reason
        }
    }

    override fun publish(
        topic: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?,
        onPublished: ((Long) -> Unit)?
    ) = publisher.publish(topic, arguments, argumentsKw, onPublished)

    override fun subscribe(
        topicPattern: UriPattern,
        eventHandler: EventHandler
    ) = subscriber.subscribe(topicPattern, eventHandler)
}

// Can be overridden to handle exceptions
interface ExceptionCatcher {
    fun catchException(error: WampErrorException)
}

internal class ExceptionSwallower : ExceptionCatcher {
    override fun catchException(error: WampErrorException) {
        // swallow
    }
}