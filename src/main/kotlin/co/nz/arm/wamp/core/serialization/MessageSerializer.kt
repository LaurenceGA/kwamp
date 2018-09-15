package co.nz.arm.wamp.core.serialization

import co.nz.arm.wamp.core.messages.Message

interface MessageSerializer {
    fun deserialize(rawMessage: ByteArray): Message
    fun serialize(message: Message): ByteArray
}