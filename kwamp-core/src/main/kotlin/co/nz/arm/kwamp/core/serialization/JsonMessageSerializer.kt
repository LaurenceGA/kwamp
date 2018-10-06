package co.nz.arm.kwamp.core.serialization

import co.nz.arm.kwamp.core.InvalidMessageException
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.isWhole
import co.nz.arm.kwamp.core.messages.Message
import co.nz.arm.kwamp.core.messages.MessageType
import com.beust.klaxon.*
import java.io.StringReader
import java.nio.charset.Charset

class JsonMessageSerializer : MessageSerializer {
    override fun deserialize(rawMessage: ByteArray): Message {
        val messageArray = parseRawMessage(rawMessage.toString(Charset.defaultCharset()))
        val (messageType, data) = extractMessageType(messageArray)
        val factory = MessageType.getFactory(messageType)
        return factory.invoke(data)
    }

    private fun parseRawMessage(rawMessage: String) = try {
        Klaxon().parseArrayWithMapConverter(rawMessage.reader())
    } catch (e: KlaxonException) {
        throw InvalidMessageException("Couldn't parse message", e)
    }

    private fun extractMessageType(messageArray: List<Any>) = try {
        Pair(toInteger(messageArray[0] as Number), messageArray.drop(1))
    } catch (e: IndexOutOfBoundsException) {
        throw InvalidMessageException("Message must have a least one item", e)
    } catch (e: ClassCastException) {
        throw InvalidMessageException("Message type must be a number", e)
    }

    private fun toInteger(num: Number): Int =
        num.toInt()
            .takeIf { num.isWhole() }
            ?: throw InvalidMessageException("Message type must be an integer")

    override fun serialize(message: Message) = Klaxon()
        .converter(Uri.UriConverter)
        .converter(MessageType.MessageTypeConverter)
        .toJsonString(message.asList()).toByteArray()
}

internal fun Klaxon.parseArrayWithMapConverter(reader: StringReader) =
    converter(co.nz.arm.kwamp.core.serialization.MAP_CONVERTER).fromJsonArray(parseJsonArray(reader))

internal fun Klaxon.fromJsonArray(jsonArray: JsonArray<*>) = jsonArray.map(this::convertJsonObject)

internal fun Klaxon.convertJsonObject(jo: Any?): Any = when {
    jo is JsonObject -> parseFromJsonObject<Map<String, Any?>>(jo)
        ?: throw KlaxonException("Couldn't convert $jo")
    jo != null -> {
        val converter = findConverterFromClass(Any::class.java, null)
        converter.fromJson(JsonValue(jo, null, null, this))
    }
    else -> throw KlaxonException("Couldn't convert $jo")
}

private val MAP_CONVERTER = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == Map::class.java
    override fun toJson(value: Any): String = ""
    override fun fromJson(jv: JsonValue): Map<String, Any?> = HashMap(jv.obj!!).mapValues(this::convertEntry)

    private fun convertEntry(entry: Map.Entry<String, Any?>) = Klaxon().converter(this).convertJsonObject(entry.value)
}