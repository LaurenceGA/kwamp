package co.nz.arm.wamp.core.messages

import co.nz.arm.wamp.core.canBeAppliedToType
import co.nz.arm.wamp.core.isWhole
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

fun generateFactory(messageClass: KClass<out Message>): (objectArray: List<Any>) -> Message = { inputList ->
    val parameters = messageClass.primaryConstructor!!.parameters
    val mappedInputList = mapInputListTypesToParameters(inputList, parameters)

    messageClass::primaryConstructor.get()!!.callBy(parameters.zip(mappedInputList).toMap())
}

private fun acceptableNumberOfParameters(parameters: List<KParameter>) = numberOfNonOptional(parameters)..parameters.size

private fun numberOfNonOptional(parameters: List<KParameter>) = parameters.count { !it.isOptional }

private fun mapInputListTypesToParameters(inputList: List<Any>, parameters: List<KParameter>) =
        if (inputList.size in acceptableNumberOfParameters(parameters))
            conformArrayObjectsToConstructor(inputList, parameters)
        else
            throw IllegalArgumentException("Not enough parameters in message")

private fun conformArrayObjectsToConstructor(inputArray: List<Any>, parameters: List<KParameter>) = inputArray.mapIndexed { index, item ->
    if (inputArray[index].canBeAppliedToType(parameters[index]))
        item
    else
        tryToConvert(item, parameters[index])
}

private fun canApplyValuesToParameters(values: List<Any>, parameters: List<KParameter>) = values.indices.all { i ->
    values[i].canBeAppliedToType(parameters[i])
}

private fun getUnaryParameterConstructor(input: Any, parameter: KParameter): KFunction<Any> = try {
    parameter.type.jvmErasure.constructors.first { areValidParameterValues(listOf(input), it.parameters) }
} catch (e: NoSuchElementException) {
    throw IllegalArgumentException("Couldn't find a way to create ${parameter.type.jvmErasure.simpleName} from $input (${input::class.simpleName})", e)
}

private fun areValidParameterValues(objectArray: List<Any>, constructorParameters: List<KParameter>) =
        objectArray.size in acceptableNumberOfParameters(constructorParameters)
                && canApplyValuesToParameters(objectArray, constructorParameters)

private fun tryToConvert(item: Any, parameter: KParameter): Any {
    return try {
        getUnaryParameterConstructor(item, parameter).call(item)
    } catch (e: IllegalArgumentException) {
        if (item is Double && item.isWhole()) {
            when(parameter.type.jvmErasure) {
                Long::class -> item.toLong()
                Int::class -> item.toInt()
                else -> throw e
            }
        } else {
            throw e
        }
    }
}