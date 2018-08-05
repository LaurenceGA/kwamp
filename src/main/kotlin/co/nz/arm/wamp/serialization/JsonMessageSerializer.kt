package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.messages.Message
import co.nz.arm.wamp.messages.MessageType
import com.beust.klaxon.Klaxon

class JsonMessageSerializer : MessageSerializer {
    override fun deserialize(rawMessage: String): Message {
        val messageArray = Klaxon().parseArray<Any>(rawMessage)
        return MessageType.getFactory(messageArray!![0] as Int)?.invoke(messageArray.subList(1, messageArray.size))!!
    }

    override fun serialize(message: Message) = Klaxon().toJsonString(message.toList())
}