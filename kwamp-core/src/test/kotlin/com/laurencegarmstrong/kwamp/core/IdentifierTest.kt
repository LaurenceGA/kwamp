package com.laurencegarmstrong.kwamp.core

import io.kotlintest.be
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class IdentifierTest : StringSpec({
    "Generate linear id" {
        val linearIdGenerator = LinearIdGenerator()
        for (i in 1..100) {
            linearIdGenerator.newId() shouldBe i
        }
    }

    "Ids can be released and skipped if not" {
        val someGenerator = object : WampIdGenerator() {
            override val sequence = listOf(1L, 2L, 1L, 2L, 3L, 2L, 4L).asSequence()
        }
        val testSet = IdentifiableSet<Any>(someGenerator)

        val id1 = testSet.put(Any())
        id1 should be(1L)
        val id2 = testSet.put(Any())
        id2 should be(2L)
        testSet.remove(id1)

        val id3 = testSet.put(Any())
        id3 should be(1L)

        // id2 (2) has not been released, so should skip to 3
        val id4 = testSet.put(Any())
        id4 should be(3L)

        testSet.remove(id2)
        val id5 = testSet.put(Any())
        id5 should be(2L)
    }
})