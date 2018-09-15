package co.nz.arm.wamp

import co.nz.arm.wamp.core.canBeAppliedToType
import co.nz.arm.wamp.core.isWhole
import co.nz.arm.wamp.core.splitAt
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType

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

    "can be applied to parameter (int to Long)" {
        1.canBeAppliedToType(object : KParameter {
            override val annotations = emptyList<Annotation>()
            override val index = 0
            override val isOptional = false
            override val isVararg = false
            override val kind = KParameter.Kind.VALUE
            override val name = "someLong"
            override val type = Long::class.createType()

        }) shouldBe true
    }

    "Double is whole" {
        forall(
                row(0.0, true),
                row(1.0, true),
                row(-1.0, true),
                row(2.0, true),
                row(50.0, true),
                row(-50.0, true),
                row(124321350351.0, true),
                row(-0.1, false),
                row(-123124312.513513124, false),
                row(103.000005, false),
                row(-1353.12412, false)

        ) { num, whole ->
            num.isWhole() shouldBe whole
        }
    }
})