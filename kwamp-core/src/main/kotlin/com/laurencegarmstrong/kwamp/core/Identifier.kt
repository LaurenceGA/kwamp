package com.laurencegarmstrong.kwamp.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

abstract class WampIdGenerator {
    private val usedIds = ConcurrentHashMap.newKeySet<Long>()

    protected abstract val sequence: Sequence<Long>

    fun newId(): Long = sequence.first { isValid(it) }.also { usedIds.add(it) }

    private fun isValid(id: Long) = Identifier.isValid(id) && !hasId(id)

    private fun hasId(id: Long) = id in usedIds

    fun releaseId(id: Long) = usedIds.remove(id)
}

object Identifier {
    internal val acceptableRange = 1..2.pow(53)

    fun isValid(id: Long) = id in acceptableRange
}

class LinearIdGenerator(seed: Long = 1L) : WampIdGenerator() {
    override val sequence = generateSequence(seed) { (it + 1).rem(Identifier.acceptableRange.endInclusive) }
}

class RandomIdGenerator : WampIdGenerator() {
    override val sequence = generateSequence(0L) { Identifier.acceptableRange.random() }
}

fun Int.pow(x: Int) = this.toDouble().pow(x).toLong()

fun ClosedRange<Long>.random() =
    ThreadLocalRandom.current().nextLong(endInclusive + 1 - start) + start

class IdentifiableSet<T>(private val idGenerator: WampIdGenerator) {
    private val backingSet = ConcurrentHashMap<Long, T>()

    operator fun get(id: Long) = backingSet[id]

    fun putWithId(objectInitializer: (id: Long) -> T) =
        idGenerator.newId().let { id ->
            objectInitializer(id).also {
                backingSet[id] = it
            }
        }

    fun put(obj: T) = idGenerator.newId().also { id ->
        backingSet[id] = obj
    }

    fun remove(id: Long) = backingSet.remove(id)?.also {
        idGenerator.releaseId(id)
    }

    fun removeIf(predicate: (T) -> Boolean) {
        backingSet.forEach { id: Long, element: T ->
            if (predicate(element)) remove(id)
        }
    }
}