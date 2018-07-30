package co.nz.arm.wamp.messages

import co.nz.arm.wamp.URI
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

sealed class Message() {
    abstract val messageType: MessageType;
}

fun Any.canBeAppliedToType(parameter: KParameter) = this::class.isSubclassOf(parameter.type.jvmErasure)

object MessageFactoryGenerator {
    fun getFactory(messageClass: KClass<out Message>): (objectArray: List<Any>) -> Message = {
        validateArray(it, messageClass.primaryConstructor!!.parameters)
        (messageClass::primaryConstructor::get)()!!.call(*it.toTypedArray())
    }

    fun validateArray(objectArray: List<Any>, constructorParameters: List<KParameter>) {
        if (objectArray.size != constructorParameters.size || canApplyValuesToParameters(objectArray, constructorParameters))
            throw RuntimeException("Invalid message")
    }

    private fun canApplyValuesToParameters(values: List<Any>, parameters: List<KParameter>) = values.indices.none { i ->
        values[i].canBeAppliedToType(parameters[i])
    }
}

class Hello(realm: String, val details: Any) : Message() {
    override val messageType = MessageType.HELLO
    val realm: URI = URI(realm)
}

class Welcome(session: Int, details: Any) : Message() {
    override val messageType = MessageType.WELCOME
}

class Abort(details: Any, reason: URI) : Message() {
    override val messageType = MessageType.ABORT
}