package co.nz.arm.wamp.messages

import kotlin.reflect.KClass

enum class MessageType(val id: Int, val type: KClass<out Message>) { //val constructor: (List<Any>) -> MessageType) {
    HELLO(1, Hello::class),
    WELCOME(2, Welcome::class),
    ABORT(3, Abort::class),
    GOODBYE(6, Message::class),
    ERROR(8, Message::class),
    PUBLISH(16, Message::class),
    PUBLISHED(17, Message::class),
    SUBSCRIBE(32, Message::class),
    SUBSCRIBED(33, Message::class),
    UNSUBSCRIBE(34, Message::class),
    UNSUBSCRIBED(35, Message::class),
    EVENT(36, Message::class),
    CALL(48, Message::class),
    RESULT(50, Message::class),
    REGISTER(64, Message::class),
    REGISTERED(65, Message::class),
    UNREGISTER(66, Message::class),
    UNREGISTERED(67, Message::class),
    INVOCATION(68, Message::class),
    YIELD(70, Message::class);

    companion object {
        private val factories = hashMapOf(*MessageType.values().map(::toIndexedFactory).toTypedArray())

        private fun toIndexedFactory(message: MessageType) = Pair(message.id, MessageFactoryGenerator.getFactory(message.type))

        fun getFactory(id: Int) = factories[id]
    }
}