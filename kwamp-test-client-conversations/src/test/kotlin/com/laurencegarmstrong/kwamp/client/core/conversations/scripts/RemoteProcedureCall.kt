package com.laurencegarmstrong.kwamp.client.core.conversations.scripts

import com.laurencegarmstrong.kwamp.client.core.call.*
import com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure.ClientConversation
import com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure.ClientConversationCanvas
import com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure.TestClient
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.WampErrorException
import com.laurencegarmstrong.kwamp.core.messages.*
import io.kotlintest.be
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.containExactly
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.runBlocking

class RemoteProcedureCall : StringSpec({
    "A client can call a procedure" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testProcedure = Uri("test.proc")

            val callArguments = listOf(1, 2, "peanut")
            val callArgumentsKw = mapOf(
                "one" to 1,
                "three" to "two"
            )
            val deferredCallResult = client.call(testProcedure, callArguments, callArgumentsKw)
            var callRequestId: Long? = null
            client shouldHaveSentMessage { message: Call ->
                message.procedure should be(testProcedure)
                message.arguments shouldContainExactly callArguments
                message.argumentsKw!! should containExactly<String, Any?>(callArgumentsKw)
                callRequestId = message.requestId
            }

            client willBeSentRouterMessage {
                Result(callRequestId!!, emptyMap(), callArguments, callArgumentsKw)
            }

            val callResult = runBlocking {
                deferredCallResult.await()
            }

            callResult.arguments shouldContainExactly callArguments
            callResult.argumentsKw!! should containExactly<String, Any?>(callArgumentsKw)
        }
    }

    "A Client can register a procedure and have it be called" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testProcedure = Uri("test.proc")
            val registrationId = 1L
            clientRegistersAProcedure(client, testProcedure, registrationId)

            val callArguments = listOf(1, 2, "peanut")
            val callArgumentsKw = mapOf(
                "one" to 1,
                "three" to "two"
            )
            val invocationRequestId = 789L
            client willBeSentRouterMessage {
                Invocation(invocationRequestId, registrationId, emptyMap(), callArguments, callArgumentsKw)
            }

            client shouldHaveSentMessage { message: Yield ->
                message.arguments shouldContainExactly callArguments
                message.argumentsKw!! should containExactly<String, Any?>(callArgumentsKw)
                message.requestId should be(invocationRequestId)
            }
        }
    }

    "A Client can register a procedure and throw an error in it" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testProcedure = Uri("test.proc")
            val registrationId = 1L
            clientRegistersAProcedure(client, testProcedure, registrationId) { arguments, argumentsKw ->
                throw CallException(arguments, argumentsKw, Uri("error.FAIL"))
            }

            val callArguments = listOf(1, 2, "peanut")
            val callArgumentsKw = mapOf(
                "one" to 1,
                "three" to "two"
            )
            val invocationRequestId = 789L
            client willBeSentRouterMessage {
                Invocation(invocationRequestId, registrationId, emptyMap(), callArguments, callArgumentsKw)
            }

            client shouldHaveSentMessage { message: Error ->
                message.requestType should be(MessageType.INVOCATION)
                message.arguments shouldContainExactly callArguments
                message.argumentsKw!! should containExactly<String, Any?>(callArgumentsKw)
                message.error should be(Uri("error.FAIL"))
                message.requestId should be(invocationRequestId)
            }
        }
    }

    "A Client can register and unregister a procedure" {
        ClientConversation {
            val client = newConnectedTestClient()
            val registrationId = 1L
            val registrationHandle = clientRegistersAProcedure(client, Uri("test.proc"), registrationId)

            val invocationRequestId1 = 456L
            client willBeSentRouterMessage {
                Invocation(invocationRequestId1, registrationId, emptyMap())
            }
            client shouldHaveSentMessage { message: Yield ->
                message.requestId should be(invocationRequestId1)
            }

            clientUnregistersAProcedure(client, registrationHandle, registrationId)

            val invocationRequestId2 = 789L
            client willBeSentRouterMessage {
                Invocation(invocationRequestId2, registrationId, emptyMap())
            }
            client shouldHaveSentMessage { message: Error ->
                message.requestId should be(invocationRequestId2)
                message.requestType should be(MessageType.INVOCATION)
                message.error should be(DEFAULT_INVOCATION_ERROR)
            }
        }
    }

    "A Client can receive an exception if a call fails" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testProcedure = Uri("test.proc")

            val callArguments = listOf(1, 2, "peanut")
            val callArgumentsKw = mapOf(
                "one" to 1,
                "three" to "two"
            )
            val deferredCallResult = client.call(testProcedure, callArguments, callArgumentsKw)
            var callRequestId: Long? = null
            client shouldHaveSentMessage { message: Call ->
                message.procedure should be(testProcedure)
                message.arguments shouldContainExactly callArguments
                message.argumentsKw!! should containExactly<String, Any?>(callArgumentsKw)
                callRequestId = message.requestId
            }

            val invocationErrorArguments = listOf(3, 4, "didn't work")
            val invocationErrorArgumentsKw = mapOf(
                "five" to 7,
                "six" to "nope"
            )
            val invocationErrorUri = Uri("nope.did.not.work")
            client willBeSentRouterMessage {
                Error(
                    MessageType.CALL,
                    callRequestId!!,
                    emptyMap(),
                    invocationErrorUri,
                    invocationErrorArguments,
                    invocationErrorArgumentsKw
                )
            }

            runBlocking {
                try {
                    deferredCallResult.await()
                } catch (error: WampErrorException) {
                    error.requestId should be(callRequestId!!)
                    error.requestType should be(MessageType.CALL)
                    error.arguments shouldContainExactly invocationErrorArguments
                    error.argumentsKw!! should containExactly<String, Any?>(invocationErrorArgumentsKw)
                }
            }
        }
    }
})

private fun ClientConversationCanvas.clientRegistersAProcedure(
    client: TestClient,
    procedure: Uri,
    registrationId: Long,
    block: CallHandler = { arguments, argumentsKw ->
        // Echo procedure
        CallResult(arguments, argumentsKw)
    }
): RegistrationHandle {
    val deferredRegistrationHandle = asyncWithTimeout {
        client.register(procedure, block)
    }

    var requestId: Long? = null
    client shouldHaveSentMessage { message: Register ->
        message.procedure should be(procedure)
        requestId = message.requestId
    }
    client willBeSentRouterMessage { Registered(requestId!!, registrationId) }

    return runBlocking {
        deferredRegistrationHandle.await()
    }
}

private fun ClientConversationCanvas.clientUnregistersAProcedure(
    client: TestClient,
    registrationHandle: RegistrationHandle,
    registrationId: Long
) {
    launchWithTimeout {
        registrationHandle.unregister()
    }
    var requestId: Long? = null
    client shouldHaveSentMessage { message: Unregister ->
        message.registration should be(registrationId)
        requestId = message.requestId
    }

    client willBeSentRouterMessage { Unregistered(requestId!!) }
}
