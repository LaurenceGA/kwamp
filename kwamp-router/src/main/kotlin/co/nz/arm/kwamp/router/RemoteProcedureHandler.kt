package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.*
import co.nz.arm.kwamp.core.messages.Register
import co.nz.arm.kwamp.core.messages.Unregister
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RemoteProcedureHandler(
    private val messageSender: MessageSender,
    private val linearIdGenerator: LinearIdGenerator
) {
    private val procedureLock = ReentrantLock()
    private val procedures = HashMap<Uri, Long>()
    private val procedureRegistrations = HashMap<Long, ProcedureConfig>()

    fun registerProcedure(
        connection: Connection,
        registrationMessage: Register
    ) {
        if (registrationMessage.procedure in procedures) throw ProcedureAlreadyExistsException(registrationMessage.requestId)

        linearIdGenerator.newId().let { registrationId ->
            procedureLock.withLock {
                procedures[registrationMessage.procedure] = registrationId
                procedureRegistrations[registrationId] = ProcedureConfig(registrationMessage.procedure, connection)
            }
            messageSender.sendRegistered(connection, registrationMessage.requestId, registrationId)
        }
    }

    fun unregisterProcedure(connection: Connection, unregisterMessage: Unregister) {
        procedureLock.withLock {
            val procedureConfig = procedureRegistrations.remove(unregisterMessage.registration)
                ?: throw NoSuchRegistrationErrorException(unregisterMessage.requestId)

            procedures.remove(procedureConfig.uri) ?: throw IllegalStateException("Couldn't find stored procedure URI")
        }
        messageSender.sendUnregistered(connection, unregisterMessage.requestId)
    }
}

data class ProcedureConfig(val uri: Uri, val procedureProvidingConnection: Connection)