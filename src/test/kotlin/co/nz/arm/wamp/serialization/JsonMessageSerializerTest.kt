package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.messages.*
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class JsonMessageSerializerTest : StringSpec({
    val messageSerializer = JsonMessageSerializer()
    "Serialize Messages" {
        forall(
                row(Hello(Uri("testRealm"), "details"), "[1, \"testRealm\", \"details\"]"),
                row(Welcome(123, "details"), "[2, 123, \"details\"]"),
                row(Abort("details", Uri("reason")), "[3, \"details\", \"reason\"]"),
                row(Goodbye("details", Uri("reason")), "[6, \"details\", \"reason\"]"),
                row(Error(5, 123, "details", Uri("error")), "[8, 5, 123, \"details\", \"error\"]"),
                row(Publish(123, "options", Uri("topic")), "[16, 123, \"options\", \"topic\"]"),
                row(Published(123, 456), "[17, 123, 456]")
        ) { message, rawMessage ->
            messageSerializer.serialize(message) shouldBe rawMessage
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
})