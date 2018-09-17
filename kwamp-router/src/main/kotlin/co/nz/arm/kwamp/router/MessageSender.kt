package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.Connection
import co.nz.arm.kwamp.core.WampClose
import co.nz.arm.kwamp.core.WampException
import co.nz.arm.kwamp.core.messages.Abort
import co.nz.arm.kwamp.core.messages.Goodbye
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