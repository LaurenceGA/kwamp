package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.core.InvalidMessageException
import co.nz.arm.wamp.core.serialization.MessagePackSerializer
import com.daveanthonythomas.moshipack.MoshiPack
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import java.nio.charset.Charset

class MessagePackMessageSerializerTest : StringSpec({
    val messageSerializer = MessagePackSerializer()

    "Serialize  messages" {
        forall(*messageData.toTypedArray()) { message, rawMessageJson ->
            messageSerializer.serialize(message).toString(Charset.defaultCharset()) shouldBe MoshiPack().jsonToMsgpack(rawMessageJson).readString(Charset.defaultCharset())
        }
    }

    "Deserialize messages" {
        forall(*messageData.toTypedArray()) { message, rawMessageJson ->
            messageSerializer.deserialize(MoshiPack.jsonToMsgpack(rawMessageJson).readByteArray()) shouldBe message
        }
    }

    "Unknown message type" {
        shouldThrow<InvalidMessageException> {
            messageSerializer.deserialize(MoshiPack.jsonToMsgpack("[-1.0, {}]").readByteArray())
        }
    }

    "Incorrect messageType type" {
        shouldThrow<InvalidMessageException> {
            messageSerializer.deserialize(MoshiPack.jsonToMsgpack("[\"NAN\", {}]").readByteArray())
        }
    }
})