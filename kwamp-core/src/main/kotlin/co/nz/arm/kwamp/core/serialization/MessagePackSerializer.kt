package co.nz.arm.kwamp.core.serialization

import co.nz.arm.kwamp.core.InvalidMessageException
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.isWhole
import co.nz.arm.kwamp.core.messages.Message
import co.nz.arm.kwamp.core.messages.MessageType
import com.daveanthonythomas.moshipack.MoshiPack

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
            add(MessageType.MessageTypeJsonAdapter)
            add(Uri.UriJsonAdapter)
        }).packToByteArray(message.asList())
}