package com.laurencegarmstrong.kwamp.core.serialization

import com.laurencegarmstrong.kwamp.core.messages.Message

interface MessageSerializer {
    fun deserialize(rawMessage: ByteArray): Message
    fun serialize(message: Message): ByteArray
}