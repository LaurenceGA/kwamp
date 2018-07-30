package co.nz.arm.wamp.messages

import co.nz.arm.wamp.URI

sealed class Message(val messageType: MessageType) {
    override fun toString() = "[$messageType]"
}

class Hello(realm: String, val details: Any) : Message(MessageType.HELLO) {
    val realm = URI(realm)
    override fun toString() = "[$messageType, $realm, $details]"
}

class Welcome(val session: Int, val details: Any) : Message(MessageType.WELCOME) {
    override fun toString() = "[$messageType, $session, $details]"
}

class Abort(val details: Any, reason: String) : Message(MessageType.ABORT) {
    val reason = URI(reason)

    override fun toString() = "[$messageType, $details, $reason]"
}