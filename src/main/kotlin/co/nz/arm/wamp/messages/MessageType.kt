package co.nz.arm.wamp.messages

// TODO merge into Messages.kt
enum class MessageType(val id: Int, val factory: (List<Any>) -> Message) {
    HELLO(1, generateFactory(Hello::class)),
    WELCOME(2, generateFactory(Welcome::class)),
    ABORT(3, generateFactory(Abort::class)),
    GOODBYE(6, generateFactory(Goodbye::class)),
    ERROR(8, generateFactory(Error::class)),
    PUBLISH(16, generateFactory(Publish::class)),
    PUBLISHED(17, generateFactory(Published::class)),
    SUBSCRIBE(32, generateFactory(Subscribe::class)),
    SUBSCRIBED(33, generateFactory(Subscribed::class)),
    UNSUBSCRIBE(34, generateFactory(Unsubscribe::class)),
    UNSUBSCRIBED(35, generateFactory(Unsubscribed::class)),
    EVENT(36, generateFactory(Event::class)),
    CALL(48, generateFactory(Call::class)),
    RESULT(50, generateFactory(Result::class)),
    REGISTER(64, generateFactory(Register::class)),
    REGISTERED(65, generateFactory(Registered::class)),
    UNREGISTER(66, generateFactory(Unregister::class)),
    UNREGISTERED(67, generateFactory(Unregistered::class)),
    INVOCATION(68, generateFactory(Invocation::class)),
    YIELD(70, generateFactory(Yield::class));

    companion object {
        private val factories = hashMapOf(*MessageType.values().map(::toIndexedFactory).toTypedArray())

        private fun toIndexedFactory(message: MessageType) = Pair(message.id, message.factory)

        fun getFactory(id: Int) = factories[id]
    }
}