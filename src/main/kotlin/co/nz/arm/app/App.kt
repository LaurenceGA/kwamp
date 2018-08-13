package co.nz.arm.app

import co.nz.arm.wamp.*
import co.nz.arm.wamp.router.Realm
import co.nz.arm.wamp.router.Router
import co.nz.arm.wamp.serialization.JsonMessageSerializer
import co.nz.arm.wamp.serialization.MessagePackSerializer
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.*
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import java.time.Duration

private val router = Router().also { it.addRealm(Realm(Uri("default"))) }

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(30)
    }

    routing {
        webSocket("/connect") {
            startWampSession(this, WAMP_DEFAULT)
        }
        webSocket("/connect", WAMP_JSON) {
            startWampSession(this, WAMP_JSON)
        }
        webSocket("/connect", WAMP_MSG_PACK) {
            startWampSession(this, WAMP_MSG_PACK)
        }
    }
}

private suspend fun Application.startWampSession(session: DefaultWebSocketServerSession, protocol: String) = session.apply{
    log.info("Websocket connection established. It's JSON!")
    val wampIncoming = Channel<ByteArray>()
    val wampOutgoing = Channel<ByteArray>()

    val connection = Connection(wampIncoming, wampOutgoing, { message -> close(CloseReason(CloseReason.Codes.NORMAL, message)) }, getSerializer(protocol))

    router.registerConnection(connection)


    launch {
        wampOutgoing.consumeEach { message ->
            send(Frame.Text(message.toString()))
        }
        log.info("Websocket no longer forwarding messages")
    }

    incoming.consumeEach { frame ->
        if (frame is Frame.Text && protocol == WAMP_JSON) {
            wampIncoming.send(frame.readText().toByteArray())
        } else if (frame is Frame.Binary && protocol == WAMP_MSG_PACK) {
            wampIncoming.send(frame.buffer.array())
        }
    }

    log.info("Websocket connection ended")
}

private fun getSerializer(protocol: String) = when(protocol) {
    WAMP_JSON -> JsonMessageSerializer()
    WAMP_MSG_PACK -> MessagePackSerializer()
    else -> throw IllegalArgumentException("'$protocol' is not a supported protocol")
}