package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.InvalidMessageException
import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.messages.Message
import co.nz.arm.wamp.messages.MessageType
import com.beust.klaxon.*
import java.io.StringReader

class JsonMessageSerializer : MessageSerializer {
    override fun deserialize(rawMessage: String): Message {
        val messageArray = parseRawMessage(rawMessage)
        val (messageType, data) = extractMessageType(messageArray)
        val factory = MessageType.getFactory(messageType)
        return factory.invoke(data)
    }

    private fun parseRawMessage(rawMessage: String) = try {
        Klaxon().parseArrayWithMapConverter(rawMessage.reader())
    } catch (e: KlaxonException) {
        throw InvalidMessageException("Couldn't parse message", e)
    }

    private fun Klaxon.parseArrayWithMapConverter(reader: StringReader) = converter(MAP_CONVERTER).fromJsonArray(parseJsonArray(reader))

    private fun Klaxon.fromJsonArray(jsonArray: JsonArray<*>): List<Any> {
        val result = arrayListOf<Any>()
        jsonArray.forEach { jo ->
            if (jo is JsonObject) {
                val t = parseFromJsonObject<Map<String, Any?>>(jo)
                if (t != null) result.add(t)
                else throw KlaxonException("Couldn't convert $jo")
            } else if (jo != null) {
                val converter = findConverterFromClass(Any::class.java, null)
                val convertedValue = converter.fromJson(JsonValue(jo, null, null, this))
                result.add(convertedValue)
            } else {
                throw KlaxonException("Couldn't convert $jo")
            }
        }

        return result
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

private val MAP_CONVERTER = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == Map::class.java
    override fun toJson(value: Any): String = ""
    override fun fromJson(jv: JsonValue): Map<String, Any?> = HashMap(jv.obj!!)
}