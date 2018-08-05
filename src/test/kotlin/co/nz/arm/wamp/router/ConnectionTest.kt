package co.nz.arm.wamp.router

import co.nz.arm.wamp.messages.Hello
import kotlinx.coroutines.experimental.channels.Channel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionTest {
    @Test
    suspend fun sendHello() {
        val incoming = Channel<String>()
        val outgoing = Channel<String>()
        val connection = Connection(incoming, outgoing, { message -> Unit })

        connection.send(Hello("realm", ""))

        connection.onNextMessage {
            println(it)
        }
    }
}