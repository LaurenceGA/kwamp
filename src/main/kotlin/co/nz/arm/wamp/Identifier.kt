package co.nz.arm.wamp

import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

object Identifier {
    private val minId = 0L
    private val maxId = 2.0.pow(53).toLong()

    fun newRandom(): Long = (minId..maxId).random()
}

fun ClosedRange<Long>.random() =
        ThreadLocalRandom.current().nextLong(endInclusive+1 - start) +  start