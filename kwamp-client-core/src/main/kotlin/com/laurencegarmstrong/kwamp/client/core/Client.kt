package com.laurencegarmstrong.kwamp.client.core

import com.laurencegarmstrong.kwamp.client.core.call.*
import com.laurencegarmstrong.kwamp.client.core.pubsub.Publisher
import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.*
import com.laurencegarmstrong.kwamp.core.serialization.json.JsonMessageSerializer
import com.laurencegarmstrong.kwamp.core.serialization.messagepack.MessagePackSerializer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

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
}

class ClientImpl(
    incoming: ReceiveChannel<ByteArray>,
    outgoing: SendChannel<ByteArray>,
    realm: Uri,
    protocol: String = WAMP_DEFAULT
) : Client {
    private val log = LoggerFactory.getLogger(ClientImpl::class.java)!! //TODO make this cleaner extension?
    //TODO bubble close function up to transport layer
    private val connection = Connection(incoming, outgoing, {}, getSerializer(protocol))

    private var sessionId: Long? = null

    private val randomIdGenerator = RandomIdGenerator()

    private val messageListenersHandler = MessageListenersHandler()

    private val caller = Caller(connection, randomIdGenerator, messageListenersHandler)
    private val callee = Callee(connection, randomIdGenerator, messageListenersHandler)

    private val publisher = Publisher(connection, randomIdGenerator, messageListenersHandler)

    init {
        joinRealm(realm)

        //TODO handle errors gracefully
        GlobalScope.launch {
            connection.forEachMessage(exceptionHandler(connection)) {
                try {
                    handleMessage(it)
                } catch (nonFatalError: WampErrorException) {
//                    messageSender.sendExceptionError(connection, nonFatalError)
                }
            }.invokeOnCompletion { fatalException ->
                fatalException?.run { printStackTrace() }
//                when (fatalException) {
////                    is ProtocolViolationException -> messageSender.sendAbort(connection, fatalException)
//                    else -> fatalException?.run { printStackTrace() }
//                }
//                sessions.endSession(id)
            }
        }
    }

    private fun exceptionHandler(connection: Connection): (Throwable) -> Unit = { throwable ->
        when (throwable) {
            is WampErrorException -> {
            }//messageSender.sendExceptionError(connection, throwable)
            else -> throw throwable
        }
    }

    override fun register(procedure: Uri, handler: CallHandler) =
        callee.register(procedure, handler)

    private fun handleMessage(message: Message) {
        messageListenersHandler.notifyListeners(message)

        when (message) {
            is Invocation -> callee.invokeProcedure(message)

            is Error -> handleError(message)    // Need to check request type?
        }
    }

    private fun handleError(errorMessage: Error) {
        when (errorMessage.requestType) {
//            MessageType.CALL -> caller.error(errorMessage)

//            else -> throw NotImplementedError("Error with request type ${errorMessage.requestType} not implemented")
        }
    }

    private fun getSerializer(protocol: String) =
        when (protocol) {
            WAMP_JSON -> JsonMessageSerializer()
            WAMP_MSG_PACK -> MessagePackSerializer()
            else -> throw IllegalArgumentException("Unsupported sub protocol '$protocol'")
        }

    private fun joinRealm(realmUri: Uri) = runBlocking {
        connection.send(Hello(realmUri, emptyMap()))
        connection.withNextMessage { message: Welcome ->
            log.info("Session established. ID: ${message.session}")
            //TODO thread safety?
            sessionId = message.session
        }.join()
    }

    //TODO make extension on Client?
    override fun call(
        procedure: Uri,
        arguments: List<Any?>?,
        argumentsKw: Dict?
    ) = caller.call(procedure, arguments, argumentsKw)

    override fun disconnect(closeReason: Uri) = runBlocking {
        connection.send(Goodbye(emptyMap(), closeReason))

        messageListenersHandler.registerListener<Goodbye>().await().let { message ->
            log.info("Router replied goodbye reason: ${message.reason}")
            message.reason
        }
    }

    override fun publish(topic: Uri, arguments: List<Any?>?, argumentsKw: Dict?, onPublished: ((Long) -> Unit)?) =
        publisher.publish(topic, arguments, argumentsKw, onPublished)
}