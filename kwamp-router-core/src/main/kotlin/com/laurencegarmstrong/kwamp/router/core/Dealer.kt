package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Dealer(
    private val messageSender: MessageSender,
    private val linearIdGenerator: LinearIdGenerator,
    randomIdGenerator: RandomIdGenerator
) {
    private val procedureLock = ReentrantLock()
    private val procedures = HashMap<Uri, Long>()
    private val procedureRegistrations = HashMap<Long, ProcedureConfig>()

    private val invocations = IdentifiableSet<InvocationConfig>(randomIdGenerator)


    fun registerProcedure(
        session: WampSession,
        registrationMessage: Register
    ) {
        if (registrationMessage.procedure in procedures)
            throw ProcedureAlreadyExistsException(registrationMessage.requestId)

        linearIdGenerator.newId().also { registrationId ->
            procedureLock.withLock {
                procedures[registrationMessage.procedure] = registrationId
                procedureRegistrations[registrationId] =
                        ProcedureConfig(
                            registrationMessage.procedure,
                            session,
                            registrationId
                        )
            }
            messageSender.sendRegistered(session.connection, registrationMessage.requestId, registrationId)
        }
    }

    fun unregisterProcedure(session: WampSession, unregisterMessage: Unregister) {
        procedureLock.withLock {
            //TODO clear associated invocations
            val procedureConfig = procedureRegistrations.remove(unregisterMessage.registration)
                ?: throw NoSuchRegistrationErrorException(unregisterMessage.requestId)

            //TODO release ID after use from linear id generator
            procedures.remove(procedureConfig.uri)!!
        }
        messageSender.sendUnregistered(session.connection, unregisterMessage.requestId)
    }

    fun callProcedure(callerSession: WampSession, callMessage: Call) {
        val procedureConfig = getProcedureConfig(callMessage)
        val invocationRequestId = invocations.put(
            InvocationConfig(
                callMessage.requestId,
                callerSession,
                procedureConfig.procedureProvidingSession
            )
        )
        messageSender.sendInvocation(
            procedureConfig.procedureProvidingSession.connection,
            invocationRequestId,
            procedureConfig.registrationId,
            emptyMap(),
            callMessage.arguments,
            callMessage.argumentsKw
        )
    }

    private fun getProcedureConfig(callMessage: Call) = procedureLock.withLock {
        procedureRegistrations[procedures[callMessage.procedure]]
            ?: throw NoSuchProcedureException(callMessage.requestId)
    }

    fun handleYield(yieldMessage: Yield) {
        //TODO  handle no invocation found
        val invocationConfig = invocations.remove(yieldMessage.requestId)!!
        messageSender.sendResult(
            invocationConfig.caller.connection,
            invocationConfig.callRequestId,
            emptyMap(),
            yieldMessage.arguments,
            yieldMessage.argumentsKw
        )
    }

    fun handleInvocationError(errorMessage: Error) {
        //TODO handle invocation doesn't exist
        val invocationConfig = invocations.remove(errorMessage.requestId)!!
        messageSender.sendCallError(
            invocationConfig.caller.connection,
            invocationConfig.callRequestId,
            emptyMap(),
            errorMessage.error,
            errorMessage.arguments,
            errorMessage.argumentsKw
        )
    }
}

data class ProcedureConfig(val uri: Uri, val procedureProvidingSession: WampSession, val registrationId: Long)

data class InvocationConfig(
    val callRequestId: Long,
    val caller: WampSession,
    val callee: WampSession
)
