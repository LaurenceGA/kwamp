package co.nz.arm.kwamp.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

abstract class WampIdGenerator {
    private val usedIds = ConcurrentHashMap.newKeySet<Long>()

    protected abstract val sequence: Sequence<Long>

    fun newId(): Long = sequence.first { isValid(it) }

    private fun isValid(id: Long) = Identifier.isValid(id) && !hasId(id)

    private fun hasId(id: Long) = id in usedIds

    fun releaseId(id: Long) = usedIds.remove(id)
}

object Identifier {
    val acceptableRange = 1..2.pow(53)

    fun isValid(id: Long) = id in acceptableRange
}

fun Int.pow(x: Int) = this.toDouble().pow(x).toLong()

class LinearIdGenerator(private val seed: Long = 1L) : WampIdGenerator() {
    override val sequence = generateSequence(seed) { (it + 1).rem(Identifier.acceptableRange.endInclusive) }
}

class RandomIdGenerator() : WampIdGenerator() {
    override val sequence = generateSequence(0L) { Identifier.acceptableRange.random() }
}

fun ClosedRange<Long>.random() =
    ThreadLocalRandom.current().nextLong(endInclusive + 1 - start) + start