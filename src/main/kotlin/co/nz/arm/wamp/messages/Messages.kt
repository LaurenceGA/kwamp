package co.nz.arm.wamp.messages

import co.nz.arm.wamp.URI

sealed class Message(val messageType: MessageType)

class Hello(realm: String, val details: Any) : Message(MessageType.HELLO) {
    val realm: URI = URI(realm)
}

class Welcome(session: Int, details: Any) : Message(MessageType.WELCOME)

class Abort(details: Any, reason: URI) : Message(MessageType.ABORT)