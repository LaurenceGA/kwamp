package co.nz.arm.wamp.router

import co.nz.arm.wamp.core.Connection
import co.nz.arm.wamp.core.WampClose
import co.nz.arm.wamp.core.WampException
import co.nz.arm.wamp.core.messages.Abort
import co.nz.arm.wamp.core.messages.Goodbye
import kotlinx.coroutines.experimental.launch

class MessageSender {
    fun sendGoodbye(connection: Connection) = launch {
        connection.send(Goodbye(mapOf(), WampClose.GOODBYE_AND_OUT.uri))
        connection.close("Closed by client.")
    }

    fun abort(connection: Connection, exception: WampException) = launch {
        connection.send(Abort(mapOf("message" to exception.localizedMessage), exception.error.uri))
        connection.close(exception.localizedMessage)
    }
}