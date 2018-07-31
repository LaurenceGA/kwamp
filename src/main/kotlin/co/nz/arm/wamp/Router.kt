package co.nz.arm.wamp

import co.nz.arm.wamp.messages.*
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch

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
    private var status = SessionStatus.ESTABLISHING

    init {
        this.close = { message ->
            outgoing.close()
            closeConnection(message)
        }

        launch {
            incoming.consumeEach {rawMessage ->
                val messageArray = Klaxon().parseArray<Any>(rawMessage)
                val message = MessageType.getFactory(messageArray!![0] as Int)?.invoke(messageArray.subList(1, messageArray.size))
                println("Received: (${message!!::class.simpleName}) ${message}")
                when (message) {
                    is Hello -> routeHello(message)
                }

            }
        }
    }

    private suspend fun routeHello(message: Hello) {
        when (status) {
            SessionStatus.ESTABLISHING -> establishSession()
            SessionStatus.ESTABLISHED -> protocolViolation("Received HELLO message after session was established.")
        }
    }

    private suspend fun establishSession() {
        send(Welcome(1, ""))
        status = SessionStatus.ESTABLISHED
    }

    private suspend fun protocolViolation(message: String) {
        send(Abort("{}", message))
        close(message)
    }

    private suspend  fun send(message: Message) {
        outgoing.send(message.toString())
    }
}

enum class SessionStatus {
    ESTABLISHING,
    ESTABLISHED
}