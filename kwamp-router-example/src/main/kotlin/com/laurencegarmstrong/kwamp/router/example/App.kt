package com.laurencegarmstrong.kwamp.router.example

import co.nz.arm.kwamp.core.*
import co.nz.arm.kwamp.core.serialization.JsonMessageSerializer
import co.nz.arm.kwamp.core.serialization.MessagePackSerializer
import co.nz.arm.kwamp.router.Realm
import co.nz.arm.kwamp.router.Router
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.time.Duration

private val router = Router().also { it.addRealm(Realm(Uri("default"))) }
private const val websocketPath = "/connect"

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(30)
    }

    routing {
        webSocket(websocketPath) {
            startWampSession(this, WAMP_DEFAULT)
        }
        webSocket(websocketPath, WAMP_JSON) {
            startWampSession(this, WAMP_JSON)
        }
        webSocket(websocketPath, WAMP_MSG_PACK) {
            startWampSession(this, WAMP_MSG_PACK)
        }
    }
}

private suspend fun Application.startWampSession(session: DefaultWebSocketServerSession, protocol: String) =
    session.apply {
        log.info("Websocket connection established. It's JSON!")
        val wampIncoming = Channel<ByteArray>()
        val wampOutgoing = Channel<ByteArray>()

        val connection = Connection(
            wampIncoming,
            wampOutgoing,
            { message ->
                flush()
                close(CloseReason(CloseReason.Codes.NORMAL, message))
            },
            getSerializer(protocol)
        )

        router.registerConnection(connection)

        GlobalScope.launch {
            wampOutgoing.consumeEach { message ->
                log.info("Sending: ${message.toString(Charsets.UTF_8)}")
                send(Frame.Text(message.toString(Charsets.UTF_8)))
            }
            log.info("Websocket no longer forwarding messages")
        }

        incoming.consumeEach { frame ->
            if (frame is Frame.Text && protocol == WAMP_JSON) {
                log.info("Received: ${frame.readText()}")
                wampIncoming.send(frame.readText().toByteArray())
            } else if (frame is Frame.Binary && protocol == WAMP_MSG_PACK) {
                wampIncoming.send(frame.buffer.array())
            }
        }

        log.info("Websocket connection ended")
    }

private fun getSerializer(protocol: String) = when (protocol) {
    WAMP_JSON -> JsonMessageSerializer()
    WAMP_MSG_PACK -> MessagePackSerializer()
    else -> throw IllegalArgumentException("'$protocol' is not a supported protocol")
}