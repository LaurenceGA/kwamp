package com.laurencegarmstrong.kwamp.core.messages

import com.laurencegarmstrong.kwamp.core.*

/**
 * During serialization messages are created reflectively using [generateFactory] based on their primary constructor
 */
sealed class Message(val messageType: MessageType) {
    fun asList(): List<Any?> = listOf(messageType) + this.primaryConstructorValues().filter { it != null }
    fun validateUris(strict: Boolean = false) = try {
        this.primaryConstructorValues().mapNotNull { it as? UriPattern }
            .forEach(if (strict) UriPattern::ensureStrict else UriPattern::ensureValid)
    } catch (invalidUri: InvalidUriException) {
        throw InvalidUriErrorException(this)
    }
}

interface RequestMessage {
    val requestId: Long
}

typealias Dict = Map<String, Any?>

data class Hello(val realm: Uri, val details: Dict) : Message(MessageType.HELLO)

data class Welcome(val session: Long, val details: Dict) : Message(MessageType.WELCOME)

data class Abort(val details: Dict, val reason: Uri) : Message(MessageType.ABORT)

data class Goodbye(val details: Dict, val reason: Uri) : Message(MessageType.GOODBYE)

data class Error(
    val requestType: MessageType,
    override val requestId: Long,
    val details: Dict,
    val error: Uri,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.ERROR), RequestMessage

data class Publish(
    override val requestId: Long,
    val options: Dict,
    val topic: Uri,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.PUBLISH), RequestMessage

data class Published(override val requestId: Long, val publication: Long) : Message(MessageType.PUBLISHED), RequestMessage

data class Subscribe(override val requestId: Long, val options: Dict, val topic: UriPattern) : Message(MessageType.SUBSCRIBE), RequestMessage

data class Subscribed(override val requestId: Long, val subscription: Long) : Message(MessageType.SUBSCRIBED), RequestMessage

data class Unsubscribe(override val requestId: Long, val subscription: Long) : Message(MessageType.UNSUBSCRIBE), RequestMessage

data class Unsubscribed(override val requestId: Long) : Message(MessageType.UNSUBSCRIBED), RequestMessage

data class Event(
    val subscription: Long,
    val publication: Long,
    val details: Dict,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.EVENT)

data class Call(
    override val requestId: Long,
    val options: Dict,
    val procedure: Uri,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.CALL), RequestMessage

data class Result(
    override val requestId: Long,
    val details: Dict,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.RESULT), RequestMessage

data class Register(override val requestId: Long, val options: Dict, val procedure: Uri) : Message(MessageType.REGISTER), RequestMessage

data class Registered(override val requestId: Long, val registration: Long) : Message(MessageType.REGISTERED), RequestMessage

data class Unregister(override val requestId: Long, val registration: Long) : Message(MessageType.UNREGISTER), RequestMessage

data class Unregistered(override val requestId: Long) : Message(MessageType.UNREGISTERED), RequestMessage

data class Invocation(
    override val requestId: Long,
    val registration: Long,
    val details: Dict,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.INVOCATION), RequestMessage

data class Yield(
    override val requestId: Long,
    val options: Dict,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.YIELD), RequestMessage