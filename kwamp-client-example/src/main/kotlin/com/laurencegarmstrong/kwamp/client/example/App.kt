package com.laurencegarmstrong.kwamp.client.example

import com.laurencegarmstrong.kwamp.client.core.ClientImpl
import com.laurencegarmstrong.kwamp.client.core.call.CallResult
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.WAMP_DEFAULT
import com.laurencegarmstrong.kwamp.core.WAMP_JSON
import com.laurencegarmstrong.kwamp.core.WAMP_MSG_PACK
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
import org.slf4j.LoggerFactory

object App {
    private val log = LoggerFactory.getLogger(App::class.java)!!

    @JvmStatic
    fun main(args: Array<String>) {
        val wampClient = createWebsocketWampClient()

        var counter = 0
        val registrationHandle = wampClient.register(Uri("test.proc")) { arguments, argumentsKw ->
            counter++
            println(counter)
            CallResult(arguments, argumentsKw)
        }
        println("registered")

        while (counter < 2) {
        }
        println("Over 2 invocations")
        registrationHandle.unregister()

        val call = wampClient.call(Uri("test.proc.await"))
        runBlocking {
            println(call.await())
        }

        wampClient.disconnect()
    }

    private fun createWebsocketWampClient(): ClientImpl {
        val wampIncoming = Channel<ByteArray>()
        val wampOutgoing = Channel<ByteArray>()
        establishWebsocketConnection(wampIncoming, wampOutgoing)
        return ClientImpl(wampIncoming, wampOutgoing, Uri("default"))
    }

    private fun establishWebsocketConnection(
        wampIncoming: Channel<ByteArray>,
        wampOutgoing: Channel<ByteArray>,
        protocol: String = WAMP_DEFAULT
    ) {
        runBlocking {
            GlobalScope.launch {
                val client = websocketClient()
                client.ws(host = "localhost", port = 8080, path = "/connect") {
                    GlobalScope.launch {
                        wampOutgoing.consumeEach { message ->
                            send(Frame.Text(message.toString(Charsets.UTF_8)))
                        }
                    }

                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text && protocol == WAMP_JSON) {
                            wampIncoming.send(frame.readText().toByteArray())
                        } else if (frame is Frame.Binary && protocol == WAMP_MSG_PACK) {
                            wampIncoming.send(frame.buffer.array())
                        }
                    }
                }
            }
        }
    }

    private fun websocketClient() = HttpClient(CIO).config { install(WebSockets) }
}
