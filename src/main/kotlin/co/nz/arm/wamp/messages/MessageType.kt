package co.nz.arm.wamp.messages

import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

enum class MessageType(val id: Int, val factory: (List<Any>) -> Message) {
    HELLO(1, getFactory<Hello>()),
    WELCOME(2, getFactory<Welcome>()),
    ABORT(3, getFactory<Abort>()),
    GOODBYE(6, getFactory<Message>()),
    ERROR(8, getFactory<Message>()),
    PUBLISH(16, getFactory<Message>()),
    PUBLISHED(17, getFactory<Message>()),
    SUBSCRIBE(32, getFactory<Message>()),
    SUBSCRIBED(33, getFactory<Message>()),
    UNSUBSCRIBE(34, getFactory<Message>()),
    UNSUBSCRIBED(35, getFactory<Message>()),
    EVENT(36, getFactory<Message>()),
    CALL(48, getFactory<Message>()),
    RESULT(50, getFactory<Message>()),
    REGISTER(64, getFactory<Message>()),
    REGISTERED(65, getFactory<Message>()),
    UNREGISTER(66, getFactory<Message>()),
    UNREGISTERED(67, getFactory<Message>()),
    INVOCATION(68, getFactory<Message>()),
    YIELD(70, getFactory<Message>());

    companion object {
        private val factories = hashMapOf(*MessageType.values().map(::toIndexedFactory).toTypedArray())

        private fun toIndexedFactory(message: MessageType) = Pair(message.id, message.factory)

        fun getFactory(id: Int) = factories[id]
    }
}