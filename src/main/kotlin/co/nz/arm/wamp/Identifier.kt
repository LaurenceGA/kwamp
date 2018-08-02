package co.nz.arm.wamp

import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

interface WampIdGenerator {
    fun newId(): Long
}

object Identifier {
    val acceptableRange = 1..2.pow(53)

    fun isValid(id: Long) = id in acceptableRange
}

fun Int.pow(x: Int) = this.toDouble().pow(x).toLong()

class LinearIdGenerator() : WampIdGenerator {
    private var nextId = 1L
    override fun newId() = ++nextId
}

class RandomIdGenerator() : WampIdGenerator {
    override fun newId() = Identifier.acceptableRange.random()
}

fun ClosedRange<Long>.random() =
        ThreadLocalRandom.current().nextLong(endInclusive+1 - start) +  start