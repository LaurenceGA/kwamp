package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.messages.Message
import co.nz.arm.wamp.messages.MessageType
import com.beust.klaxon.Klaxon

class JsonMessageSerializer : MessageSerializer {
    override fun deserialize(rawMessage: String): Message {
        val messageArray = parseRawMessage(rawMessage)
        val (messageType, data) = extractMessageType(messageArray)
        val factory = MessageType.getFactory(messageType)
        return factory.invoke(data)
    }

    private fun parseRawMessage(rawMessage: String) = Klaxon().parseArray<Any>(rawMessage) ?: throw RuntimeException("Invalid parse")

    private fun extractMessageType(messageArray: List<Any>) = try {
        Pair(messageArray[0] as Int, messageArray.drop(1))
    } catch (e: ArrayIndexOutOfBoundsException) {
        throw RuntimeException("MessageArray must have a least one item")
    } catch (e: ClassCastException) {
        throw RuntimeException("Message type must be an integer")
    }

    override fun serialize(message: Message) = Klaxon()
            .converter(Uri.UriConverter)
            .converter(MessageType.MessageTypeConverter)
            .toJsonString(message.toList())
}