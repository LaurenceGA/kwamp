package nz.co.arm.kwamp.client.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nz.co.arm.kwamp.client.Client
import org.slf4j.LoggerFactory

object App {
    private val log = LoggerFactory.getLogger(App::class.java)!!

    @JvmStatic
    fun main(args: Array<String>) {
        val wampIncoming = Channel<ByteArray>()
        val wampOutgoing = Channel<ByteArray>()
        val wampClient = Client(wampIncoming, wampOutgoing)
        establishWebsocketConnection(wampIncoming, wampOutgoing)

        wampClient.joinRealm("default")
    }

    private fun establishWebsocketConnection(wampIncoming: Channel<ByteArray>, wampOutgoing: Channel<ByteArray>) {
        runBlocking {
            GlobalScope.launch {
                val client = websocketClient()
                //TODO pass URL vars as parameter
                client.ws(host = "localhost", port = 8080, path = "/connect") {
                    GlobalScope.launch {
                        wampOutgoing.consumeEach { message ->
                            log.info("Sending: ${message.toString(Charsets.UTF_8)}")
                            send(Frame.Text(message.toString(Charsets.UTF_8)))
                        }
                    }

                    incoming.consumeEach { frame ->
                        //TODO support messagePack protocol
                        if (frame is Frame.Text) {
                            log.info("Received: ${frame.readText()}")
                            wampIncoming.send(frame.readText().toByteArray())
                        }
                    }
                }
            }
        }
    }

    private fun websocketClient() = HttpClient(CIO).config { install(WebSockets) }
}
