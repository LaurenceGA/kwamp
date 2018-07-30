package co.nz.arm.wamp

import co.nz.arm.wamp.messages.Hello
import co.nz.arm.wamp.messages.Message
import co.nz.arm.wamp.messages.MessageType
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import java.io.StringReader

class Router {
//    private val connections = ConcurrentHashMap<String, MutableList<WAMPSession>>()

    suspend fun newConnection(incoming: ReceiveChannel<String>, outgoing: SendChannel<String>, close: suspend (message: String) -> Unit) {
//        connections.computeIfAbsent(id) {
//            CopyOnWriteArrayList<WebSocketSession>()
//        }.apply { add(session) }
//    }
        println("Router setting up new connection!")
        WAMPSession(incoming, outgoing, close)
    }
}

class WAMPSession(val incoming: ReceiveChannel<String>, val outgoing: SendChannel<String>, closeConnection: suspend (message: String) -> Unit) {
    private val close: suspend (message: String) -> Unit
    private val status = SessionStatus.ESTABLISHING

    init {
        this.close = { message ->
            outgoing.close()
            closeConnection(message)
        }

        launch {
            incoming.consumeEach {rawMessage ->
                val messageArray = Klaxon().parseArray<Any>(rawMessage)
                val message = MessageType.getFactory(messageArray!![0] as Int)?.invoke(messageArray.subList(1, messageArray.size))
                when (message) {
                    is Hello -> println("HULLO")
                }

//                val msgId = messageArray[0]
//                if (msgId is Int) {
//                    val message = Klaxon().parseFromJsonArray<>(messageArray) as Message
//                    routeMessage(message)
//                }
            }
        }
    }

    suspend fun routeMessage(message: Message) {
        try {
//            val parsedMessage = Klaxon().parseJsonArray(StringReader(""))

//            println("msg = ${parsedMessage}, so msg type = ${parsedMessage[0]}")
//            if (parsedMessage[0] == MessageType.HELLO.id) {
//                outgoing.send("[2, 123, {}]")
//            }
        } catch (e: RuntimeException) {
            close(e.message.toString())
        }
    }
}

enum class SessionStatus {
    ESTABLISHING,
    ESTABLISHED
}