package co.nz.arm.wamp.serialization

import co.nz.arm.wamp.messages.Hello
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class JsonMessageSerializerTest : StringSpec({
    "SerializeMessages" {
        val messageSerializer = JsonMessageSerializer()
        forall(
                row(Hello("testRealm", "detail"), "[1, \"testRealm\", \"detail\"]")
        ) { message, rawMessage ->
            messageSerializer.serialize(message) shouldBe rawMessage
        }
    }
})