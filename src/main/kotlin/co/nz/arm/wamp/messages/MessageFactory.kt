package co.nz.arm.wamp.messages

import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

inline fun <reified messageClass: Message> getFactory(): (objectArray: List<Any>) -> Message = {
    validateArray(it, messageClass::class.primaryConstructor!!.parameters)
    (messageClass::class::primaryConstructor::get)()!!.call(*it.toTypedArray())
}

fun validateArray(objectArray: List<Any>, constructorParameters: List<KParameter>) {
    if (objectArray.size != constructorParameters.size || canApplyValuesToParameters(objectArray, constructorParameters))
        throw RuntimeException("Invalid message")
}

private fun canApplyValuesToParameters(values: List<Any>, parameters: List<KParameter>) = values.indices.none { i ->
    values[i].canBeAppliedToType(parameters[i])
}

fun Any.canBeAppliedToType(parameter: KParameter) = this::class.isSubclassOf(parameter.type.jvmErasure)