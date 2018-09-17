package co.nz.arm.kwamp.core.serialization

import co.nz.arm.kwamp.core.InvalidMessageException
import com.beust.klaxon.Klaxon
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import java.nio.charset.Charset

class JsonMessageSerializerTest : StringSpec({
    val messageSerializer = JsonMessageSerializer()

    "Serialize  messages" {
        forall(*messageData.toTypedArray()) { message, rawMessageJson ->
            messageSerializer.serialize(message).toString(Charset.defaultCharset()) shouldBe rawMessageJson
        }
    }

    "Deserialize messages" {
        forall(*messageData.toTypedArray()) { message, rawMessageJson ->
            messageSerializer.deserialize(rawMessageJson.toByteArray()) shouldBe message
        }
    }

    "Unknown message type" {
        shouldThrow<InvalidMessageException> {
            messageSerializer.deserialize("[-1, {}]".toByteArray())
        }
    }

    "Incorrect messageType type" {
        shouldThrow<InvalidMessageException> {
            messageSerializer.deserialize("[\"NAN\", {}]".toByteArray())
        }
    }

    "!Klaxon" {
        Klaxon().toJsonString(listOf(1, 2, null, null, 3, null)) shouldBe "[1, 2, null, null, 3, null]"
        Klaxon().toJsonString(object {
            val test = null
        }) shouldBe "{\"test\": null}"
        Klaxon().toJsonString(SomeTest(null)) shouldBe "{\"test\": null}"
    }
})

class SomeTest(val test: Any?)