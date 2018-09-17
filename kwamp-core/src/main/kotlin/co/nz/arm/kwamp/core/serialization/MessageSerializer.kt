package co.nz.arm.kwamp.core.serialization

import co.nz.arm.kwamp.core.messages.Message

interface MessageSerializer {
    fun deserialize(rawMessage: ByteArray): Message
    fun serialize(message: Message): ByteArray
}