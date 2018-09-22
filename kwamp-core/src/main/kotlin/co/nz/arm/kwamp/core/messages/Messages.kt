package co.nz.arm.kwamp.core.messages

import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.primaryConstructorValues

sealed class Message(val messageType: MessageType) {
    fun asList(): List<Any?> = listOf(messageType).plus(this.primaryConstructorValues().filter { it != null })
}

typealias Dict = Map<String, Any?>

data class Hello(val realm: Uri, val details: Dict) : Message(MessageType.HELLO)

data class Welcome(val session: Long, val details: Dict) : Message(MessageType.WELCOME)

data class Abort(val details: Dict, val reason: Uri) : Message(MessageType.ABORT)

data class Goodbye(val details: Dict, val reason: Uri) : Message(MessageType.GOODBYE)

data class Error(
    val requestType: Int,
    val requestId: Long,
    val details: Dict,
    val error: Uri,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.ERROR)

data class Publish(
    val requestId: Long,
    val options: Dict,
    val topic: Uri,
    val arguments: List<Any?>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.PUBLISH)

data class Published(val requestId: Long, val publication: Long) : Message(MessageType.PUBLISHED)

data class Subscribe(val requestId: Long, val options: Dict, val topic: Uri) : Message(MessageType.SUBSCRIBE)

data class Subscribed(val requestId: Long, val subscription: Long) : Message(MessageType.SUBSCRIBED)

data class Unsubscribe(val requestId: Long, val subscription: Long) : Message(MessageType.UNSUBSCRIBE)

data class Unsubscribed(val requestId: Long) : Message(MessageType.UNSUBSCRIBED)

data class Event(
    val subscription: Long,
    val publication: Long,
    val details: Dict,
    val arguments: List<Any>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.EVENT)

data class Call(
    val requestId: Long,
    val options: Dict,
    val procedure: Uri,
    val arguments: List<Any>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.CALL)

data class Result(
    val requestId: Long,
    val details: Dict,
    val arguments: List<Any>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.RESULT)

data class Register(val requestId: Long, val options: Dict, val procedure: Uri) : Message(MessageType.REGISTER)

data class Registered(val requestId: Long, val registration: Long) : Message(MessageType.REGISTERED)

data class Unregister(val requestId: Long, val registration: Long) : Message(MessageType.UNREGISTER)

data class Unregistered(val requestId: Long) : Message(MessageType.UNREGISTERED)

data class Invocation(
    val requestId: Long,
    val options: Dict,
    val arguments: List<Any>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.INVOCATION)

data class Yield(
    val requestId: Long,
    val options: Dict,
    val arguments: List<Any>? = null,
    val argumentsKw: Dict? = null
) : Message(MessageType.YIELD)