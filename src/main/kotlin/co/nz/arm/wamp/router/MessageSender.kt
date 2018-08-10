package co.nz.arm.wamp.router

import co.nz.arm.wamp.Connection
import co.nz.arm.wamp.WampClose
import co.nz.arm.wamp.WampException
import co.nz.arm.wamp.messages.Abort
import co.nz.arm.wamp.messages.Goodbye
import kotlinx.coroutines.experimental.launch

class MessageSender {
    fun sendGoodbye(connection: Connection) = launch {
        connection.send(Goodbye("", WampClose.GOODBYE_AND_OUT.uri))
        connection.close("Closed by client.")
    }

    fun abort(connection: Connection, exception: WampException) = launch {
        connection.send(Abort(mapOf("message" to exception.localizedMessage), exception.error.uri))
        connection.close(exception.localizedMessage)
    }
}