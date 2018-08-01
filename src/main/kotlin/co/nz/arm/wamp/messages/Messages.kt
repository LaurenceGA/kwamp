package co.nz.arm.wamp.messages

import co.nz.arm.wamp.URI

sealed class Message(val messageType: MessageType) {
    override fun toString() = "[${messageType.id}]"
}

class Hello(realm: String, val details: Any) : Message(MessageType.HELLO) {
    val realm = URI(realm)
    override fun toString() = "[${messageType.id}, $realm, $details]"
}

class Welcome(val session: Int, val details: Any) : Message(MessageType.WELCOME) {
    override fun toString() = "[${messageType.id}, $session, $details]"
}

class Abort(val details: Any, reason: String) : Message(MessageType.ABORT) {
    val reason = URI(reason)

    override fun toString() = "[${messageType.id}, $details, $reason]"
}

class Goodbye(val details: Any, reason: String) : Message(MessageType.GOODBYE) {
    val reason = URI(reason)
}

class Error(val requestType: Int, val requestId: Long, val details: Any, error: String, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.ERROR) {
    val error = URI(error)
}

class Publish(val requestId: Long, val options: Any, topic: String, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.PUBLISH) {
    val topic = URI(topic)
}

class Published(val requestId: Long, val publication: Long) : Message(MessageType.PUBLISHED)

class Subscribe(val requestId: Long, val options: Any, topic: String) : Message(MessageType.SUBSCRIBE) {
    val topic = URI(topic)
}

class Subscribed(val requestId: Long, val subscription: Long) : Message(MessageType.SUBSCRIBED)

class Unsubscribe(val requestId: Long, val subscription: Long) : Message(MessageType.UNSUBSCRIBE)

class Unsubscribed(val requestId: Long) : Message(MessageType.UNSUBSCRIBED)

class Event(val subscription: Long, val publication: Long, val details: Any, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.EVENT)