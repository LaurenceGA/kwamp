package co.nz.arm.wamp

import co.nz.arm.wamp.messages.Hello
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
    private val close : suspend (message: String) -> Unit

    init {
        this.close = { message ->
            outgoing.close()
            closeConnection(message)
        }

        launch {
            incoming.consumeEach {
                println("Received message: ${it}")
                if (it == "hello") {
                    outgoing.send("Welcome and Goodbye!")
                    close("That's enough for now")
                }
            }
        }
    }
}