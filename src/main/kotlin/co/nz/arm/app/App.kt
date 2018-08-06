package co.nz.arm.app

import co.nz.arm.wamp.Connection
import co.nz.arm.wamp.router.Realm
import co.nz.arm.wamp.router.Router
import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.serialization.JsonMessageSerializer
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.*
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
            log.info("Websocket connection established")
            val wampIncoming = Channel<String>()
            val wampOutgoing = Channel<String>()

            val connection = Connection(wampIncoming, wampOutgoing, { message -> close(CloseReason(CloseReason.Codes.NORMAL, message)) }, JsonMessageSerializer())

            router.registerConnection(connection)


            launch {
                wampOutgoing.consumeEach { message ->
                    send(Frame.Text(message))
                }
                log.info("Websocket no longer forwarding messages")
            }

            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    wampIncoming.send(frame.readText())
                }
            }

            log.info("Websocket connection ended")
        }
    }
}

data class SocketSession(val id: String)