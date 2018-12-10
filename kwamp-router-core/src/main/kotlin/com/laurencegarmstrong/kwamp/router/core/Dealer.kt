package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Dealer(
    private val messageSender: MessageSender,
    linearIdGenerator: LinearIdGenerator,
    randomIdGenerator: RandomIdGenerator
) {
    private val procedureLock = ReentrantLock()
    private val procedures = HashMap<Uri, Long>()
    private val procedureRegistrations = IdentifiableSet<ProcedureConfig>(linearIdGenerator)
    private val sessionProcedures = ConcurrentHashMap<Long, MutableSet<Long>>()

    private val invocations = IdentifiableSet<InvocationConfig>(randomIdGenerator)

    private val logger = LoggerFactory.getLogger(Dealer::class.java)!!

    fun registerProcedure(
        session: WampSession,
        registrationMessage: Register
    ) {
        if (registrationMessage.procedure in procedures)
            throw ProcedureAlreadyExistsException(registrationMessage.requestId)

        val procedureConfig = procedureLock.withLock {
            procedureRegistrations.putWithId { registrationId ->
                procedures[registrationMessage.procedure] = registrationId
                ProcedureConfig(
                    registrationMessage.procedure,
                    session,
                    registrationId
                )
            }
        }
        sessionProcedures.computeIfAbsent(session.id) { hashSetOf() }.add(procedureConfig.registrationId)
        messageSender.sendRegistered(session.connection, registrationMessage.requestId, procedureConfig.registrationId)
    }

    fun unregisterProcedure(session: WampSession, unregisterMessage: Unregister) {
        sessionProcedures[session.id]?.remove(unregisterMessage.registration)
        try {
            removeRegisteredProcedure(unregisterMessage.registration)
        } catch (error: NoSuchRegistrationException) {
            throw NoSuchRegistrationErrorException(unregisterMessage.requestId)
        }
        messageSender.sendUnregistered(session.connection, unregisterMessage.requestId)
    }

    private fun removeRegisteredProcedure(registrationId: Long) {
        procedureLock.withLock {
            //TODO clear associated invocations
            val procedureConfig = procedureRegistrations.remove(registrationId)
                ?: throw NoSuchRegistrationException()

            procedures.remove(procedureConfig.uri)!!
        }
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
        val procedureRegistrationId =
            procedures[callMessage.procedure] ?: throw NoSuchProcedureException(callMessage.requestId)
        procedureRegistrations[procedureRegistrationId]!!
    }

    fun handleYield(yieldMessage: Yield) {
        val invocationConfig = invocations.remove(yieldMessage.requestId)

        if (invocationConfig == null) {
            logger.warn("Got an yield for request ${yieldMessage.requestId}, but couldn't find associated invocation")
            return
        }

        messageSender.sendResult(
            invocationConfig.caller.connection,
            invocationConfig.callRequestId,
            emptyMap(),
            yieldMessage.arguments,
            yieldMessage.argumentsKw
        )
    }

    fun handleInvocationError(errorMessage: Error) {
        val invocationConfig = invocations.remove(errorMessage.requestId)

        if (invocationConfig == null) {
            logger.warn("Got an invocation error for request ${errorMessage.requestId}, but couldn't find associated invocation")
            return
        }

        messageSender.sendCallError(
            invocationConfig.caller.connection,
            invocationConfig.callRequestId,
            emptyMap(),
            errorMessage.error,
            errorMessage.arguments,
            errorMessage.argumentsKw
        )
    }

    fun cleanSessionResources(sessionId: Long) {
        sessionProcedures.remove(sessionId)?.forEach { procedureId ->
            removeRegisteredProcedure(procedureId)
        }
    }
}

class NoSuchRegistrationException : IllegalArgumentException()

data class ProcedureConfig(val uri: Uri, val procedureProvidingSession: WampSession, val registrationId: Long)

data class InvocationConfig(
    val callRequestId: Long,
    val caller: WampSession,
    val callee: WampSession
)
