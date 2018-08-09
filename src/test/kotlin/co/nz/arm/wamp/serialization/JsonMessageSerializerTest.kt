package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.Uri
import co.nz.arm.wamp.messages.*
import com.beust.klaxon.Klaxon
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class JsonMessageSerializerTest : StringSpec({
    val messageSerializer = JsonMessageSerializer()
    val messageData = listOf(
            row(Hello(Uri("testRealm"), "details"),
                    "[1, \"testRealm\", \"details\"]"),
            row(Welcome(123, "details"),
                    "[2, 123, \"details\"]"),
            row(Abort("details", Uri("reason")),
                    "[3, \"details\", \"reason\"]"),
            row(Goodbye("details", Uri("reason")),
                    "[6, \"details\", \"reason\"]"),
            row(Error(5, 123, "details", Uri("error")),
                    "[8, 5, 123, \"details\", \"error\"]"),
//            row(Error(5, 123, "details", Uri("error"), listOf("arg1", 2, null)),
//                    "[8, 5, 123, \"details\", \"error\", [\"arg1\", 2, null]]"),
//                row(Error(5, 123, "details", Uri("error"), listOf("arg1", 2, null), mapOf("arg3" to "val3", "arg4" to 4)),
//                        "[8, 5, 123, \"details\", \"error\", [\"arg1\", 2, null], {\"arg3\": \"val3\", \"arg4\": 4}]"),
            row(Error(5, 123, "details", Uri("error"), listOf("arg1", 2), mapOf("arg3" to "val3", "arg4" to 4)),
                        "[8, 5, 123, \"details\", \"error\", [\"arg1\", 2], {\"arg3\": \"val3\", \"arg4\": 4}]"),
            row(Publish(123, "options", Uri("topic")), "[16, 123, \"options\", \"topic\"]"),
            row(Published(123, 456), "[17, 123, 456]"),
            row(Subscribe(123, "options", Uri("topic")), "[32, 123, \"options\", \"topic\"]"),
            row(Subscribed(123, 456), "[33, 123, 456]"),
            row(Unsubscribe(123, 456), "[34, 123, 456]"),
            row(Unsubscribed(123), "[35, 123]"),
            row(Event(123, 456, "details"), "[36, 123, 456, \"details\"]"),
            row(Call(123, "options", Uri("procedure")), "[48, 123, \"options\", \"procedure\"]"),
            row(Result(123, "details"), "[50, 123, \"details\"]"),
            row(Register(123, "options", Uri("procedure")), "[64, 123, \"options\", \"procedure\"]"),
            row(Registered(123, 456), "[65, 123, 456]"),
            row(Unregister(123, 456), "[66, 123, 456]"),
            row(Unregistered(123), "[67, 123]"),
            row(Invocation(123, "options"), "[68, 123, \"options\"]"),
            row(Yield(123, "options"), "[70, 123, \"options\"]")
    )

    "Serialize  messages" {
        forall(*messageData.toTypedArray()) { message, rawMessage ->
            messageSerializer.serialize(message) shouldBe rawMessage
        }
    }

    "Deserialize messages" {
        forall(*messageData.toTypedArray()) { message, rawMessage ->
            messageSerializer.deserialize(rawMessage) shouldBe message
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

    "!Klaxon" {
        Klaxon().toJsonString(listOf(1, 2, null, null, 3, null)) shouldBe "[1, 2, null, null, 3, null]"
        Klaxon().toJsonString(object {
            val test = null
        }) shouldBe "{\"test\": null}"
        Klaxon().toJsonString(SomeTest(null)) shouldBe "{\"test\": null}"
    }
})

class SomeTest(val test: Any?)