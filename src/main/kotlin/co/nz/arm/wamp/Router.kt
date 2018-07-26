package co.nz.arm.wamp

import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class Router {
    private val connections = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

    suspend fun newSession(id: String, session: WebSocketSession) = connections.computeIfAbsent(id) {
        CopyOnWriteArrayList<WebSocketSession>()
    }.apply { add(session) }

    suspend fun consume(frame: Frame) {

    }
}

class WAMPSession()