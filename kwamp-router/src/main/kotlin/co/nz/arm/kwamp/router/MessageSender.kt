package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.Connection
import co.nz.arm.kwamp.core.WampClose
import co.nz.arm.kwamp.core.WampException
import co.nz.arm.kwamp.core.messages.Abort
import co.nz.arm.kwamp.core.messages.Goodbye
import co.nz.arm.kwamp.core.messages.Registered
import co.nz.arm.kwamp.core.messages.Unregistered
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MessageSender {
    fun sendGoodbye(connection: Connection) = GlobalScope.launch {
        connection.send(Goodbye(mapOf(), WampClose.GOODBYE_AND_OUT.uri))
        connection.close("Closed by client.")
    }

    fun abort(connection: Connection, exception: WampException) = GlobalScope.launch {
        connection.send(Abort(mapOf("message" to exception.localizedMessage), exception.error.uri))
        connection.close(exception.localizedMessage)
    }

    fun sendRegistered(connection: Connection, requestId: Long, registrationId: Long) = GlobalScope.launch {
        connection.send(Registered(requestId, registrationId))
    }

    fun sendUnregistered(connection: Connection, requestId: Long) = GlobalScope.launch {
        connection.send(Unregistered(requestId))
    }
}