package co.nz.arm.wamp.messages

import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.readProperty
import kotlin.reflect.full.primaryConstructor

sealed class Message(val messageType: MessageType) {
    abstract fun toList(): List<Any?>
    fun asList(): List<Any?> = listOf(messageType).plus(this::class.primaryConstructor!!.parameters.map { this.readProperty(it.name!!) }.filter { it != null })
}

data class Hello(val realm: Uri, val details: Any) : Message(MessageType.HELLO) {
    override fun toList() = listOf(messageType.id, realm, details)
}

data class Welcome(val session: Long, val details: Any) : Message(MessageType.WELCOME) {
    override fun toList() = listOf(messageType.id, session, details)
}

data class Abort(val details: Any, val reason: Uri) : Message(MessageType.ABORT) {
    override fun toList() = listOf(messageType.id, details, reason)
}

data class Goodbye(val details: Any, val reason: Uri) : Message(MessageType.GOODBYE) {
    override fun toList() = listOf(messageType.id, reason)
}

data class Error(val requestType: Int, val requestId: Long, val details: Any, val error: Uri, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.ERROR) {
    override fun toList() = listOf(messageType.id, requestId, details, error, arguments, argumentsKw)
}

data class Publish(val requestId: Long, val options: Any, val topic: Uri, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.PUBLISH) {
    override fun toList() = listOf(messageType.id, requestId, options, topic, arguments, argumentsKw)
}

data class Published(val requestId: Long, val publication: Long) : Message(MessageType.PUBLISHED) {
    override fun toList() = listOf(messageType.id, requestId, publication)
}

data class Subscribe(val requestId: Long, val options: Any, val topic: Uri) : Message(MessageType.SUBSCRIBE) {
    override fun toList() = listOf(messageType.id, requestId, options, topic)
}

data class Subscribed(val requestId: Long, val subscription: Long) : Message(MessageType.SUBSCRIBED) {
    override fun toList() = listOf(messageType.id, requestId, subscription)
}

data class Unsubscribe(val requestId: Long, val subscription: Long) : Message(MessageType.UNSUBSCRIBE) {
    override fun toList() = listOf(messageType.id, requestId, subscription)
}

data class Unsubscribed(val requestId: Long) : Message(MessageType.UNSUBSCRIBED) {
    override fun toList() = listOf(messageType.id, requestId)
}

data class Event(val subscription: Long, val publication: Long, val details: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.EVENT) {
    override fun toList() = listOf(messageType.id, subscription, publication, details, arguments, argumentsKw)
}

data class Call(val requestId: Long, val options: Any, val procedure: Uri, val arguments: List<Any> = emptyList(), val argumentsKw: Any? = null) : Message(MessageType.CALL) {
    override fun toList() = listOf(messageType.id, requestId, options, procedure, arguments, argumentsKw)
}

data class Result(val requestId: Long, val details: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.RESULT) {
    override fun toList() = listOf(messageType.id, requestId, details, arguments, argumentsKw)
}

data class Register(val requestId: Long, val options: Any, val procedure: Uri) : Message(MessageType.REGISTER) {
    override fun toList() = listOf(messageType.id, requestId, options, procedure)
}

data class Registered(val requestId: Long, val registration: Long) : Message(MessageType.REGISTERED) {
    override fun toList() = listOf(messageType.id, requestId, registration)
}

data class Unregister(val requestId: Long, val registration: Long) : Message(MessageType.UNREGISTER) {
    override fun toList() = listOf(messageType.id, requestId, registration)
}

data class Unregistered(val requestId: Long, val registration: Long, val details: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.UNREGISTERED) {
    override fun toList() = listOf(messageType.id, requestId, registration, details, arguments, argumentsKw)
}

data class Invocation(val requestId: Long, val options: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.INVOCATION) {
    override fun toList() = listOf(messageType.id, requestId, options, arguments, argumentsKw)
}

data class Yield(val requestId: Long, val options: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.YIELD) {
    override fun toList() = listOf(messageType.id, requestId, options, arguments, argumentsKw)
}