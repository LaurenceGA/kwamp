package co.nz.arm.wamp.router

import co.nz.arm.wamp.Connection
import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.WampException
import co.nz.arm.wamp.messages.Abort
import kotlinx.coroutines.experimental.launch

suspend fun Connection.sendProtocolViolation(uri: Uri, message: String) = this.apply {
    send(Abort("{}", uri))
    close(message)
}

fun Connection.sendProtocolViolation(exception: WampException) = launch {
    send(Abort(mapOf("message" to exception.localizedMessage), exception.error.uri))
    close(exception.localizedMessage)
}