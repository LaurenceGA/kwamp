package co.nz.arm.app

import co.nz.arm.wamp.Router
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.nextNonce
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
        install(Sessions) {
            cookie<SocketSession>("SESSION")
        }

        intercept(ApplicationCallPipeline.Infrastructure) {
            if (call.sessions.get<SocketSession>() == null) {
                call.sessions.set(SocketSession(nextNonce()))
            }
        }

        webSocket("/register") {
            val session = call.sessions.get<SocketSession>()
            if (session== null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                return@webSocket
            }

            log.info("session=${session.id}")

            try {
                incoming.consumeEach {frame ->
                    if (frame is Frame.Text) {
                        log.info(frame.readText())
                        router.consume(frame)
                    }
                }
            } finally {
                log.info("Disconnected (${session.id})")
            }
        }

        get("/") {
            call.respondText("Hello World")
        }
    }
}

data class SocketSession(val id: String)