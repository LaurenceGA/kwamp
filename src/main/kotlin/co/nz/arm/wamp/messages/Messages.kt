package co.nz.arm.wamp.messages

import co.nz.arm.wamp.MessageType
import co.nz.arm.wamp.URI

sealed class Message() {
    abstract val messageType : MessageType;
}

class Hello(realm: URI, details: Any) : Message() {
    override val messageType = MessageType.HELLO
}

class Welcome(session: Int, details: Any) : Message() {
    override val messageType = MessageType.WELCOME
}

class Abort(details: Any, reason: URI) : Message() {
    override val messageType = MessageType.ABORT
}