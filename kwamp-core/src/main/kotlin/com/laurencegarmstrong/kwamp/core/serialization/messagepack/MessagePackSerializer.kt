package com.laurencegarmstrong.kwamp.core.serialization.messagepack

import com.daveanthonythomas.moshipack.MoshiPack
import com.laurencegarmstrong.kwamp.core.InvalidMessageException
import com.laurencegarmstrong.kwamp.core.isWhole
import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.messages.MessageType
import com.laurencegarmstrong.kwamp.core.serialization.MessageSerializer

class MessagePackSerializer : MessageSerializer {
    override fun deserialize(rawMessage: ByteArray): Message {
        val messageArray = parseRawMessage(rawMessage)
        val (messageType, data) = extractMessageType(messageArray)
        val factory = MessageType.getFactory(messageType)
        return factory.invoke(data)
    }

    private fun parseRawMessage(rawMessage: ByteArray) = try {
        MoshiPack.unpack<List<Any>>(rawMessage)
    } catch (e: IllegalStateException) {
        throw InvalidMessageException("Couldn't parse message", e)
    }

    private fun extractMessageType(messageArray: List<Any>) = try {
        Pair(toInteger(messageArray[0] as Double), messageArray.drop(1))
    } catch (e: IndexOutOfBoundsException) {
        throw InvalidMessageException("Message must have a least one item", e)
    } catch (e: ClassCastException) {
        throw InvalidMessageException("Message type must be a number", e)
    }

    private fun toInteger(num: Double): Int =
        num.toInt()
            .takeIf { num.isWhole() }
            ?: throw InvalidMessageException("Message type must be an integer")

    override fun serialize(message: Message): ByteArray =
        MoshiPack({
            add(MessageTypeJsonAdapter)
            add(UriJsonAdapter)
        }).packToByteArray(message.asList())
}