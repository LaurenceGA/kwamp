package co.nz.arm.wamp.messages

import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

inline fun <reified messageClass : Message> getFactory(): (objectArray: List<Any>) -> Message = {
    validateArray(it, messageClass::class.primaryConstructor!!.parameters)
    messageClass::class::primaryConstructor.get()!!.call(*it.toTypedArray())
}

fun validateArray(objectArray: List<Any>, constructorParameters: List<KParameter>) {
    if (!areValidParameterValues(objectArray, constructorParameters))
        throw RuntimeException("Invalid message")
}

private fun areValidParameterValues(objectArray: List<Any>, constructorParameters: List<KParameter>) =
        objectArray.size in acceptableNumberOfParameters(constructorParameters) && canApplyValuesToParameters(objectArray, constructorParameters)

private fun acceptableNumberOfParameters(parameters: List<KParameter>) = numberOfNonOptional(parameters)..parameters.size

private fun numberOfNonOptional(parameters: List<KParameter>) = parameters.count { !it.isOptional }

private fun canApplyValuesToParameters(values: List<Any>, parameters: List<KParameter>) = values.indices.all { i ->
    values[i].canBeAppliedToType(parameters[i])
}

fun Any.canBeAppliedToType(parameter: KParameter) = this::class.isSubclassOf(parameter.type.jvmErasure)