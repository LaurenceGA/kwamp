package co.nz.arm.kwamp.core

import co.nz.arm.kwamp.core.messages.Dict
import co.nz.arm.kwamp.core.messages.Error
import co.nz.arm.kwamp.core.messages.MessageType

enum class WampError(uri: String) {
    // Peer provided an incorrect URI for any URI-based attribute of WAMP message, such as realm, topic or procedure
    INVALID_URI("wamp.error.invalid_uri"),
    // A Dealer could not perform a call, since no procedure is currently registered under the given URI.
    NO_SUCH_PROCEDURE("wamp.error.no_such_procedure"),
    // A procedure could not be registered, since a procedure with the given URI is already registered.
    PROCEDURE_ALREADY_EXISTS("wamp.error.procedure_already_exists"),
    // A Dealer could not perform an unregister, since the given registration is not active.
    NO_SUCH_REGISTRATION("wamp.error.no_such_registration"),
    // A Broker could not perform an unsubscribe, since the given subscription is not active.
    NO_SUCH_SUBSCRIPTION("wamp.error.no_such_subscription"),
    // A call failed since the given argument types or values are not acceptable to the called procedure.
    INVALID_ARGUMENT("wamp.error.invalid_argument"),
    // A Peer received invalid WAMP protocol message (e.g. HELLO message after session was already established) - used as a ABORT reply reason.
    PROTOCOL_VIOLATION("wamp.error.protocol_violation"),
    // A join, call, register, publish or subscribe failed, since the Peer is not authorized to perform the operation.
    NOT_AUTHORIZED("wamp.error.not_authorized"),
    // A Dealer or Broker could not determine if the Peer is authorized to perform a join, call, register, publish or subscribe, since the authorization operation itself failed. E.g. a custom authorizer did run into an error.
    AUTHORIZATION_FAILED("wamp.error.authorization_failed"),
    // Peer wanted to join a non-existing realm (and the Router did not allow to auto-create the realm).
    NO_SUCH_REALM("wamp.error.no_such_realm"),
    // A Peer was to be authenticated under a Role that does not (or no longer) exists on the Router. For example, the Peer was successfully authenticated, but the Role configured does not exists - hence there is some misconfiguration in the Router.
    NO_SUCH_ROLE("wamp.error.no_such_role"),
    // A Dealer or Callee canceled a call previously issued
    CANCELED("wamp.error.canceled"),
    // A Peer requested an interaction with an option that was disallowed by the Router
    OPTION_NOT_ALLOWED("wamp.error.option_not_allowed"),
    // A Dealer could not perform a call, since a procedure with the given URI is registered, but Callee Black- and Whitelisting and/or Caller Exclusion lead to the exclusion of (any) Callee providing the procedure.
    NO_ELIGIBLE_CALLEE("wamp.error.no_eligible_callee"),
    // A Router rejected client request to disclose its identity
    OPTION_DISALLOWED_DISCLOSE_ME("wamp.error.option_disallowed.disclose_me"),
    // A Router encountered a network failure
    NETWORK_FAILURE("wamp.error.network_failure");


    val uri = Uri(uri)
}

open class WampException(val error: WampError, message: String? = null, cause: Throwable? = null) :
    Exception(message, cause)

class InvalidUriException(message: String? = null, cause: Throwable? = null) :
    WampException(WampError.INVALID_URI, message = message, cause = cause)

open class ProtocolViolationException(message: String? = null, cause: Throwable? = null) :
    WampException(WampError.PROTOCOL_VIOLATION, message = message, cause = cause)

class InvalidMessageException(message: String? = null, cause: Throwable? = null) :
    ProtocolViolationException(message = message, cause = cause)

class NoSuchRealmException(message: String? = null, cause: Throwable? = null) :
    WampException(WampError.NO_SUCH_REALM, message = message, cause = cause)

open class WampErrorException(
    error: WampError,
    val requestType: MessageType,
    val requestId: Long,
    private val details: Dict = emptyMap(),
    private val arguments: List<Any?>? = null,
    private val argumentsKw: Dict? = null
) :
    WampException(error) {
    fun getErrorMessage() = Error(requestType, requestId, details, error.uri, arguments, argumentsKw)
}

class ProcedureAlreadyExistsException(requestId: Long) :
WampErrorException(WampError.PROCEDURE_ALREADY_EXISTS, requestType = MessageType.REGISTER, requestId = requestId)

class NoSuchRegistrationErrorException(requestId: Long) :
    WampErrorException(WampError.NO_SUCH_REGISTRATION, requestType = MessageType.UNREGISTER, requestId = requestId)

class NoSuchProcedureException(requestId: Long) :
    WampErrorException(WampError.NO_SUCH_PROCEDURE, requestType = MessageType.CALL, requestId = requestId)

class NoSuchSubscriptionException(requestId: Long) :
    WampErrorException(WampError.NO_SUCH_SUBSCRIPTION, requestType = MessageType.UNSUBSCRIBE, requestId = requestId)
