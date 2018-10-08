package nz.co.arm.kwamp.client

import co.nz.arm.kwamp.core.*
import co.nz.arm.kwamp.core.messages.Hello
import co.nz.arm.kwamp.core.messages.Welcome
import co.nz.arm.kwamp.core.serialization.JsonMessageSerializer
import co.nz.arm.kwamp.core.serialization.MessagePackSerializer
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class Client(
    incoming: ReceiveChannel<ByteArray>,
    outgoing: SendChannel<ByteArray>,
    protocol: String = WAMP_DEFAULT
) {
    private val log = LoggerFactory.getLogger(Client::class.java)!!
    //TODO bubble close function up to transport layer
    private val connection = Connection(incoming, outgoing, {}, getSerializer(protocol))

    private fun getSerializer(protocol: String) =
        when (protocol) {
            WAMP_JSON -> JsonMessageSerializer()
            WAMP_MSG_PACK -> MessagePackSerializer()
            else -> throw IllegalArgumentException("Unsupported sub protocol '${protocol}'")
        }

    //TODO make this part of building a client
    fun joinRealm(realmUri: String) {
        runBlocking {
            connection.send(Hello(Uri(realmUri), emptyMap()))
            connection.withNextMessage { message: Welcome ->
                log.info("Session established. ID: ${message.session}")
            }.join()
        }
    }
}