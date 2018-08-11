package co.nz.arm.wamp.messages

import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.primaryConstructorValues

sealed class Message(val messageType: MessageType) {
    fun asList(): List<Any?> = listOf(messageType).plus(this.primaryConstructorValues().filter { it != null })
}

data class Hello(val realm: Uri, val details: Map<String, Any?>) : Message(MessageType.HELLO)

data class Welcome(val session: Long, val details: Any) : Message(MessageType.WELCOME)

data class Abort(val details: Any, val reason: Uri) : Message(MessageType.ABORT)

data class Goodbye(val details: Any, val reason: Uri) : Message(MessageType.GOODBYE)

data class Error(val requestType: Int, val requestId: Long, val details: Any, val error: Uri, val arguments: List<Any?>? = null, val argumentsKw: Map<String, Any?>? = null) : Message(MessageType.ERROR)

data class Publish(val requestId: Long, val options: Any, val topic: Uri, val arguments: List<Any?>? = null, val argumentsKw: Any? = null) : Message(MessageType.PUBLISH)

data class Published(val requestId: Long, val publication: Long) : Message(MessageType.PUBLISHED)

data class Subscribe(val requestId: Long, val options: Any, val topic: Uri) : Message(MessageType.SUBSCRIBE)

data class Subscribed(val requestId: Long, val subscription: Long) : Message(MessageType.SUBSCRIBED)

data class Unsubscribe(val requestId: Long, val subscription: Long) : Message(MessageType.UNSUBSCRIBE)

data class Unsubscribed(val requestId: Long) : Message(MessageType.UNSUBSCRIBED)

data class Event(val subscription: Long, val publication: Long, val details: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.EVENT)

data class Call(val requestId: Long, val options: Any, val procedure: Uri, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.CALL)

data class Result(val requestId: Long, val details: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.RESULT)

data class Register(val requestId: Long, val options: Any, val procedure: Uri) : Message(MessageType.REGISTER)

data class Registered(val requestId: Long, val registration: Long) : Message(MessageType.REGISTERED)

data class Unregister(val requestId: Long, val registration: Long) : Message(MessageType.UNREGISTER)

data class Unregistered(val requestId: Long) : Message(MessageType.UNREGISTERED)

data class Invocation(val requestId: Long, val options: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.INVOCATION)

data class Yield(val requestId: Long, val options: Any, val arguments: List<Any>? = null, val argumentsKw: Any? = null) : Message(MessageType.YIELD)