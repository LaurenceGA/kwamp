package co.nz.arm.wamp.messages

import co.nz.arm.wamp.canBeAppliedToType
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
        getUnaryParameterConstructor(item, parameters[index]).call(item)
}

private fun canApplyValuesToParameters(values: List<Any>, parameters: List<KParameter>) = values.indices.all { i ->
    values[i].canBeAppliedToType(parameters[i])
}

private fun getUnaryParameterConstructor(input: Any, parameter: KParameter): KFunction<Any> = try {
    parameter.type.jvmErasure.constructors.first { areValidParameterValues(listOf(input), it.parameters) }
} catch (e: NoSuchElementException) {
    throw IllegalArgumentException("Couldn't find a way to create ${parameter.type.jvmErasure.simpleName} from $input")
}

private fun areValidParameterValues(objectArray: List<Any>, constructorParameters: List<KParameter>) =
        objectArray.size in acceptableNumberOfParameters(constructorParameters)
                && canApplyValuesToParameters(objectArray, constructorParameters)