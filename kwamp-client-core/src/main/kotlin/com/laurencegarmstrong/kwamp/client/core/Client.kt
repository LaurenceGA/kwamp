package com.laurencegarmstrong.kwamp.client.core

import com.laurencegarmstrong.kwamp.client.core.call.CallHandler
import com.laurencegarmstrong.kwamp.client.core.call.Callee
import com.laurencegarmstrong.kwamp.client.core.call.Caller
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

class Client(
    incoming: ReceiveChannel<ByteArray>,
    outgoing: SendChannel<ByteArray>,
    realm: Uri,
    protocol: String = WAMP_DEFAULT
) {
    private val log = LoggerFactory.getLogger(Client::class.java)!! //TODO make this cleaner extension?
    //TODO bubble close function up to transport layer
    private val connection = Connection(incoming, outgoing, {}, getSerializer(protocol))

    private var sessionId: Long? = null

    private val randomIdGenerator = RandomIdGenerator()

    private val requestListenersHandler = MessageListenersHandler()

    private val caller = Caller(connection, randomIdGenerator, requestListenersHandler)
    private val callee = Callee(connection, randomIdGenerator, requestListenersHandler)

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

    fun register(procedure: Uri, handler: CallHandler) =
        callee.register(procedure, handler)

    private fun handleMessage(message: Message) {
        requestListenersHandler.notifyListeners(message)

        when (message) {
            is Result -> caller.result(message)

            is Registered -> callee.receiveRegistered(message)
            is Unregistered -> callee.receiveUnregistered(message)

            is Invocation -> callee.invokeProcedure(message)

            is Error -> handleError(message)
        }
    }

    private fun handleError(errorMessage: Error) {
        when (errorMessage.requestType) {
            MessageType.CALL -> caller.error(errorMessage)

            else -> throw NotImplementedError("Error with request type ${errorMessage.requestType} not implemented")
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
    fun call(
        procedure: Uri,
        arguments: List<Any?>? = null,
        argumentsKw: Dict? = null
    ) = caller.call(procedure, arguments, argumentsKw)

    fun disconnect(closeReason: Uri = WampClose.SYSTEM_SHUTDOWN.uri) = runBlocking {
        connection.send(Goodbye(emptyMap(), closeReason))

        requestListenersHandler.registerListener<Goodbye>().await().also { message ->
            log.info("Router replied goodbye reason: ${message.reason}")
        }
    }
}