package co.nz.arm.wamp

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldEqual
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

class ExtensionTest : StringSpec({
    "Partition a list" {
        val baseList = listOf(1, 2, 3, 4, 5)
        forall(
            row(0, Pair<List<Int>, List<Int>>(listOf(), listOf(1, 2, 3, 4, 5))),
            row(1, Pair<List<Int>, List<Int>>(listOf(1), listOf(2, 3, 4, 5))),
            row(2, Pair<List<Int>, List<Int>>(listOf(1, 2), listOf(3, 4, 5))),
            row(3, Pair<List<Int>, List<Int>>(listOf(1, 2, 3), listOf(4, 5))),
            row(4, Pair<List<Int>, List<Int>>(listOf(1, 2, 3, 4), listOf(5))),
            row(5, Pair<List<Int>, List<Int>>(listOf(1, 2, 3, 4, 5), listOf()))

        ) { partitionPoint, expectedOutcome ->
            baseList.splitAt(partitionPoint).shouldBe(expectedOutcome)
        }
    }

    "split at single item list" {
        val baseList = listOf(1)
        baseList.splitAt(1).shouldBe(Pair<List<Int>, List<Int>>(listOf(1), listOf()))
    }
})