package co.nz.arm.app

import co.nz.arm.wamp.Router
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.experimental.channels.consumeEach
import java.time.Duration

private val router = Router()

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(30)
    }

    routing {
        webSocket("/register") {
            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        log.info(frame.readText())
                        router.consume(frame)
                    }
                }
            } finally {
                log.info("Disconnected")
            }
        }

        get("/") {
            call.respondText("Hello World")
        }
    }
}

data class SocketSession(val id: String)