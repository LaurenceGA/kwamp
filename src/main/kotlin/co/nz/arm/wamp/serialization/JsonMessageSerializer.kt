package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.InvalidMessageException
import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.messages.Message
import co.nz.arm.wamp.messages.MessageType
import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException

class JsonMessageSerializer : MessageSerializer {
    override fun deserialize(rawMessage: String): Message {
        val messageArray = parseRawMessage(rawMessage)
        val (messageType, data) = extractMessageType(messageArray)
        val factory = MessageType.getFactory(messageType)
        return factory.invoke(data)
    }

    private fun parseRawMessage(rawMessage: String) = try {
        Klaxon().parseArray<Any>(rawMessage) ?: throw InvalidMessageException("Couldn't parse message into an array")
    } catch (e: KlaxonException) {
        throw InvalidMessageException("Couldn't parse message", e)
    }


    private fun extractMessageType(messageArray: List<Any>) = try {
        Pair(messageArray[0] as Int, messageArray.drop(1))
    } catch (e: IndexOutOfBoundsException) {
        throw InvalidMessageException("Message must have a least one item", e)
    } catch (e: ClassCastException) {
        throw InvalidMessageException("Message type must be an integer", e)
    }

    override fun serialize(message: Message) = Klaxon()
            .converter(Uri.UriConverter)
            .converter(MessageType.MessageTypeConverter)
            .toJsonString(message.asList())
}