package com.laurencegarmstrong.kwamp.core.serialization.json

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.laurencegarmstrong.kwamp.core.InvalidMessageException
import com.laurencegarmstrong.kwamp.core.serialization.messageData
import io.kotlintest.assertSoftly
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import java.io.StringReader
import java.nio.charset.Charset

internal class JsonMessageSerializerTest : StringSpec({
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

    "Klaxon nulls" {
        Klaxon().toJsonString(listOf(1, 2, null, null, 3, null)) shouldBe "[1, 2, null, null, 3, null]"
        Klaxon().toJsonString(object {
            val test = null
        }) shouldBe "{}"
    }

    "!Klaxon can't read" {
        assertSoftly {
            for (testStr in listOf("a", "b", "c", "as", "ag", "ab", "gs", "rs", "er")) {
                // 'as' throws IllegalArgumentException instead of KlaxonException
                shouldThrow<KlaxonException> {
                    println(testStr)
                    Klaxon().parseJsonArray(StringReader(testStr))
                }
            }
        }
    }
}) {
    class SomeTest(val test: Any?)
}
