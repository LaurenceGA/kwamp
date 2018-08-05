package co.nz.arm.wamp.messages

import co.nz.arm.wamp.URI

sealed class Message(val messageType: MessageType) {
    override fun toString() = "[${messageType.id}]"
    abstract fun toList(): List<Any>
}

class Hello(realm: String, val details: Any) : Message(MessageType.HELLO) {
    val realm = URI(realm)
    override fun toString() = "[${messageType.id}, $realm, $details]"

    override fun toList() = listOf(messageType.id, realm, details)
}

class Welcome(val session: Long, val details: Any) : Message(MessageType.WELCOME) {
    override fun toString() = "[${messageType.id}, $session, $details]"
    override fun toList() = listOf(messageType.id, session, details)
}

class Abort(val details: Any, reason: String) : Message(MessageType.ABORT) {
    val reason = URI(reason)

    override fun toString() = "[${messageType.id}, $details, $reason]"
    override fun toList() = listOf(messageType.id, details, reason)
}

class Goodbye(val details: Any, reason: String) : Message(MessageType.GOODBYE) {
    val reason = URI(reason)
    override fun toList() = listOf(messageType.id, reason)
}

class Error(val requestType: Int, val requestId: Long, val details: Any, error: String, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.ERROR) {
    val error = URI(error)

    override fun toList() = listOf(messageType.id, requestId, details, error, arguments, argumentsKw)
}

class Publish(val requestId: Long, val options: Any, topic: String, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.PUBLISH) {
    val topic = URI(topic)

    override fun toList() = listOf(messageType.id, requestId, options, topic, arguments, argumentsKw)
}

class Published(val requestId: Long, val publication: Long) : Message(MessageType.PUBLISHED) {
    override fun toList() = listOf(messageType.id, requestId, publication)
}

class Subscribe(val requestId: Long, val options: Any, topic: String) : Message(MessageType.SUBSCRIBE) {
    val topic = URI(topic)

    override fun toList() = listOf(messageType.id, requestId, options, topic)
}

class Subscribed(val requestId: Long, val subscription: Long) : Message(MessageType.SUBSCRIBED) {
    override fun toList() = listOf(messageType.id, requestId, subscription)
}

class Unsubscribe(val requestId: Long, val subscription: Long) : Message(MessageType.UNSUBSCRIBE) {
    override fun toList() = listOf(messageType.id, requestId, subscription)
}

class Unsubscribed(val requestId: Long) : Message(MessageType.UNSUBSCRIBED) {
    override fun toList() = listOf(messageType.id, requestId)
}

class Event(val subscription: Long, val publication: Long, val details: Any, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.EVENT) {
    override fun toList() = listOf(messageType.id, subscription, publication, details, arguments, argumentsKw)
}

class Call(val requestId: Long, val options: Any, procedure: String, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.CALL) {
    val procedure = URI(procedure)

    override fun toList() = listOf(messageType.id, requestId, options, procedure, arguments, argumentsKw)
}

class Result(val requestId: Long, val details: Any, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.RESULT) {
    override fun toList() = listOf(messageType.id, requestId, details, arguments, argumentsKw)
}

class Register(val requestId: Long, val options: Any, procedure: String) : Message(MessageType.REGISTER) {
    val procedure = URI(procedure)

    override fun toList() = listOf(messageType.id, requestId, options, procedure)
}

class Registered(val requestId: Long, val registration: Long) : Message(MessageType.REGISTERED) {
    override fun toList() = listOf(messageType.id, requestId, registration)
}

class Unregister(val requestId: Long, val registration: Long) : Message(MessageType.UNREGISTER) {
    override fun toList() = listOf(messageType.id, requestId, registration)
}

class Unregistered(val requestId: Long, val registration: Long, val details: Any, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.UNREGISTERED) {
    override fun toList() = listOf(messageType.id, requestId, registration, details, arguments, argumentsKw)
}

class Invocation(val requestId: Long, val options: Any, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.INVOCATION) {
    override fun toList() = listOf(messageType.id, requestId, options, arguments, argumentsKw)
}

class Yield(val requestId: Long, val options: Any, val arguments: List<Any> = emptyList(), val argumentsKw: Any = Any()) : Message(MessageType.YIELD) {
    override fun toList() = listOf(messageType.id, requestId, options, arguments, argumentsKw)
}