package co.nz.arm.wamp

import org.apache.commons.lang3.ClassUtils
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

// Partition a list by index
fun List<Any>.splitAt(index: Int) = Pair(subList(0, index), subList(index, size))

// Check if you could apply an object to a parameter on the JVM
// Includes check for java autoboxing
fun Any.canBeAppliedToType(parameter: KParameter) = this::class.isSubclassOf(parameter.type.jvmErasure)
        || ClassUtils.isAssignable(this::class.java, parameter.type.jvmErasure.java)

fun Any.readProperty(propName: String): Any? = ((this::class as KClass<Any>).declaredMemberProperties.first { it.name == propName }).get(this)

fun Any.primaryConstructorValues() = this::class.primaryConstructor!!.parameters.map { this.readProperty(it.name!!) }