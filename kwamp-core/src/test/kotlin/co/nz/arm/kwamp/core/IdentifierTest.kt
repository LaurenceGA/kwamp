package co.nz.arm.kwamp.core

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class IdentifierTest : StringSpec({
    "Generate linear id" {
        val linearIdGenerator = LinearIdGenerator()
        for (i in 0..100) {
            linearIdGenerator.newId() shouldBe i
        }
    }
})