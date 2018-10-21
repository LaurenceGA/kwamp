package com.laurencegarmstrong.kwamp.core.messages

import com.laurencegarmstrong.kwamp.core.InvalidMessageException
import kotlin.reflect.KClass

enum class MessageType(val id: Int, val messageClass: KClass<out Message>) {
    HELLO(1, Hello::class),
    WELCOME(2, Welcome::class),
    ABORT(3, Abort::class),
    GOODBYE(6, Goodbye::class),
    ERROR(8, Error::class),
    PUBLISH(16, Publish::class),
    PUBLISHED(17, Published::class),
    SUBSCRIBE(32, Subscribe::class),
    SUBSCRIBED(33, Subscribed::class),
    UNSUBSCRIBE(34, Unsubscribe::class),
    UNSUBSCRIBED(35, Unsubscribed::class),
    EVENT(36, Event::class),
    CALL(48, Call::class),
    RESULT(50, Result::class),
    REGISTER(64, Register::class),
    REGISTERED(65, Registered::class),
    UNREGISTER(66, Unregister::class),
    UNREGISTERED(67, Unregistered::class),
    INVOCATION(68, Invocation::class),
    YIELD(70, Yield::class);

    companion object {
        private val factories = hashMapOf(*MessageType.values().map(Companion::toIndexedFactory).toTypedArray())
        private val messageTypes = hashMapOf(*MessageType.values().map(Companion::toIndexedIdentity).toTypedArray())

        private fun toIndexedFactory(messageType: MessageType) =
            Pair(messageType.id, generateFactory(messageType.messageClass))

        private fun toIndexedIdentity(messageType: MessageType) =
            Pair(messageType.id, messageType)

        fun getFactory(id: Int) = factories[id]
            ?: throw InvalidMessageException("Unknown message type '$id'")

        fun getMessageType(id: Int) = messageTypes[id]
            ?: throw InvalidMessageException("Unknown message type '$id'")
    }
}