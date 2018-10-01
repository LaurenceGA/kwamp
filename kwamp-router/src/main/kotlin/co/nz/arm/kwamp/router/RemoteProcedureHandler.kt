package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.*
import co.nz.arm.kwamp.core.messages.Call
import co.nz.arm.kwamp.core.messages.Register
import co.nz.arm.kwamp.core.messages.Unregister
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// A WAMP Dealer
class RemoteProcedureHandler(
    private val messageSender: MessageSender,
    private val linearIdGenerator: LinearIdGenerator,
    private val randomIdGenerator: RandomIdGenerator
) {
    private val procedureLock = ReentrantLock()
    private val procedures = HashMap<Uri, Long>()
    private val procedureRegistrations = HashMap<Long, ProcedureConfig>()

    //TODO make own class and release generated IDs after use from random ID generator
    private val invocations = HashMap<Long, InvocationConfig>()

    fun registerProcedure(
        connection: Connection,
        registrationMessage: Register
    ) {
        if (registrationMessage.procedure in procedures) throw ProcedureAlreadyExistsException(registrationMessage.requestId)

        linearIdGenerator.newId().let { registrationId ->
            procedureLock.withLock {
                procedures[registrationMessage.procedure] = registrationId
                procedureRegistrations[registrationId] =
                        ProcedureConfig(registrationMessage.procedure, connection, registrationId)
            }
            messageSender.sendRegistered(connection, registrationMessage.requestId, registrationId)
        }
    }

    fun unregisterProcedure(connection: Connection, unregisterMessage: Unregister) {
        procedureLock.withLock {
            val procedureConfig = procedureRegistrations.remove(unregisterMessage.registration)
                ?: throw NoSuchRegistrationErrorException(unregisterMessage.requestId)

            //TODO release ID after use from linear id generator
            procedures.remove(procedureConfig.uri) ?: throw IllegalStateException("Couldn't find stored procedure URI")
        }
        messageSender.sendUnregistered(connection, unregisterMessage.requestId)
    }

    fun callProcedure(connection: Connection, callMessage: Call) {
        randomIdGenerator.newId().let { invocationRequestId ->
            val procedureConfig = getProcedureConfig(callMessage)
            messageSender.sendInvocation(
                procedureConfig.procedureProvidingConnection,
                invocationRequestId,
                procedureConfig.registrationId,
                emptyMap(),
                callMessage.arguments,
                callMessage.argumentsKw
            )

            invocations[invocationRequestId] =
                    InvocationConfig(invocationRequestId, connection, procedureConfig.procedureProvidingConnection)
        }
    }

    private fun getProcedureConfig(callMessage: Call) = procedureLock.withLock {
        procedureRegistrations[procedures[callMessage.procedure]]
            ?: throw NoSuchProcedureException(callMessage.requestId)
    }
}

data class ProcedureConfig(val uri: Uri, val procedureProvidingConnection: Connection, val registrationId: Long)

data class InvocationConfig(val invocationRequestId: Long, val caller: Connection, val callee: Connection)
