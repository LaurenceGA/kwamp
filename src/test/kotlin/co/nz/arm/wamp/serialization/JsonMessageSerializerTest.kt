package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.messages.Hello
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class JsonMessageSerializerTest : StringSpec({
    val messageSerializer = JsonMessageSerializer()
    "Serialize Messages" {
        forall(
                row(Hello(Uri("testRealm"), "detail"), "[1, \"testRealm\", \"detail\"]")
        ) { message, rawMessage ->
            messageSerializer.serialize(message) shouldBe rawMessage
        }
    }

    "Unknown message type" {
        shouldThrow<RuntimeException> {
            messageSerializer.deserialize("[-1, {}]")
        }
    }

    "Incorrect messageType type" {
        shouldThrow<RuntimeException> {
            messageSerializer.deserialize("[\"NAN\", {}]")
        }
    }

    "Deserialize Messages" {
        forall(
                row("[1, \"testRealm\", \"detail\"]", Hello(Uri("testRealm"), "detail"))
        ) { rawMessage, expectedMessage ->
            val actualMessage = messageSerializer.deserialize(rawMessage)
            actualMessage shouldBe expectedMessage
        }
    }
})