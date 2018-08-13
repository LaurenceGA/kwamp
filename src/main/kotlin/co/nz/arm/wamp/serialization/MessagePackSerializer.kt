package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.messages.Message
import com.daveanthonythomas.moshipack.MoshiPack
import java.nio.charset.Charset

class MessagePackSerializer : MessageSerializer {
    private val jsonSerializer = JsonMessageSerializer()

    override fun deserialize(rawMessage: ByteArray): Message =
            jsonSerializer
                    .deserialize(msgPackToJson(rawMessage))

    private fun msgPackToJson(rawMessage: ByteArray) =
            MoshiPack
                    .msgpackToJson(rawMessage)
                    .toByteArray(Charset.defaultCharset())

    override fun serialize(message: Message): ByteArray =
            MoshiPack
                    .jsonToMsgpack(msgToJson(message))
                    .readByteArray()

    private fun msgToJson(message: Message) =
            jsonSerializer
                    .serialize(message)
                    .toString(Charset.defaultCharset())
}