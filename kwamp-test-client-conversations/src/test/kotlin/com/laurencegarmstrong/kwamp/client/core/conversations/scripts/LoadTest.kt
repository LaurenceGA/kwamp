package com.laurencegarmstrong.kwamp.client.core.conversations.scripts

import com.laurencegarmstrong.kwamp.client.core.call.CallResult
import com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure.ClientConversation
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.*
import io.kotlintest.be
import io.kotlintest.matchers.containExactly
import io.kotlintest.matchers.haveKey
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors

class LoadTest : StringSpec({
    // Receive jobs get their own thread pool so that they don't block the send jobs
    val receiveContext =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()

    "Test client sending a high volume of publish messages" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testTopic = Uri("test.topic")
            val publishArgumentsKw = mapOf("one" to 1, "two" to "two")
            val messagesToPublish = 2000

            val deferredPublications = Array<CompletableDeferred<Long>>(messagesToPublish) {
                CompletableDeferred()
            }
            repeat(messagesToPublish) { eventId ->
                launchWithTimeout {
                    client.publish(testTopic, listOf(eventId), publishArgumentsKw) { publishId ->
                        logger.debug("Event $eventId publish acknowledged with pid=$publishId")
                        deferredPublications[eventId].complete(publishId)
                    }
                }
            }

            runBlockingWithTimeout {
                val receiveJobs = LinkedList<Job>()
                repeat(messagesToPublish) { publicationId ->
                    receiveJobs.add(launch(receiveContext) {
                        var requestId: Long? = null
                        var eventId: Int? = null
                        client shouldHaveSentMessage { message: Publish ->
                            message.topic should be(testTopic)
                            eventId = message.arguments!![0] as Int?
                            message.argumentsKw!! should containExactly<String, Any?>(publishArgumentsKw)
                            message.options should haveKey("acknowledge")
                            requestId = message.requestId
                            logger.debug("Publication $publicationId is: Publish(rid=$requestId, eid=$eventId)")
                        }

                        logger.debug("Sending client acknowledgment of publish $publicationId -> Published(rid=$requestId)")
                        client willBeSentRouterMessage { Published(requestId!!, publicationId.toLong()) }

                        logger.debug("Waiting for client to confirm event ${eventId!!} acknowledged with pid=$publicationId)")
                        deferredPublications[eventId!!].await() should be(publicationId.toLong())
                    })

                    receiveJobs.forEach {
                        it.join()
                    }
                }
            }
        }
    }

    "Test client sending a high volume of call messages" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testProcedure = Uri("test.procedure")
            val callArgumentsKw = mapOf("one" to 1, "two" to "two")
            val callToMake = 2000

            val deferredResults = Array<CompletableDeferred<Int>>(callToMake) {
                CompletableDeferred()
            }
            repeat(callToMake) { callId ->
                launchWithTimeout {
                    client.call(testProcedure, listOf(callId), callArgumentsKw).invokeOnSuccess { result ->
                        logger.debug("Call $callId succeeded with result ${result.arguments!![0]}")
                        deferredResults[callId].complete(result.arguments!![0] as Int)
                    }
                }
            }

            runBlockingWithTimeout {
                val receiveJobs = LinkedList<Job>()
                repeat(callToMake) { resultId ->
                    receiveJobs.add(launch(receiveContext) {
                        var requestId: Long? = null
                        var callId: Int? = null
                        client shouldHaveSentMessage { message: Call ->
                            message.procedure should be(testProcedure)
                            callId = message.arguments!![0] as Int?
                            message.argumentsKw!! should containExactly<String, Any?>(callArgumentsKw)
                            requestId = message.requestId
                            logger.debug("Result $resultId is: Call(rid=$requestId, cid=$callId)")
                        }

                        logger.debug("Sending client result of $resultId -> Result(rid=$requestId)")
                        client willBeSentRouterMessage { Result(requestId!!, emptyMap(), listOf(resultId)) }

                        logger.debug("Waiting for client to confirm call ${callId!!} got result=$resultId)")
                        deferredResults[callId!!].await() should be(resultId)
                    })
                }

                receiveJobs.forEach {
                    it.join()
                }
            }
        }
    }

    "Test client sending a high volume of call messages and receiving errors" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testProcedure = Uri("test.procedure")
            val testError = Uri("some.error")
            val callArgumentsKw = mapOf("one" to 1, "two" to "two")
            val callsToMake = 2000

            val deferredResults = Array<CompletableDeferred<Int>>(callsToMake) {
                CompletableDeferred()
            }
            repeat(callsToMake) { callId ->
                launchWithTimeout {
                    client.call(testProcedure, listOf(callId), callArgumentsKw).invokeOnError { exception ->
                        logger.debug("Call $callId got error with id ${exception.arguments!![0]} and uri ${exception.error}")
                        deferredResults[callId].complete(exception.arguments!![0] as Int)
                    }
                }
            }

            runBlockingWithTimeout {
                val receiveJobs = LinkedList<Job>()
                repeat(callsToMake) { errorId ->
                    receiveJobs.add(launch(receiveContext) {
                        var requestId: Long? = null
                        var callId: Int? = null
                        client shouldHaveSentMessage { message: Call ->
                            message.procedure should be(testProcedure)
                            callId = message.arguments!![0] as Int?
                            message.argumentsKw!! should containExactly<String, Any?>(callArgumentsKw)
                            requestId = message.requestId
                            logger.debug("Error $errorId is for Call(rid=$requestId, cid=$callId)")
                        }

                        logger.debug("Sending client error with error of $errorId -> Error(rid=$requestId)")
                        client willBeSentRouterMessage {
                            Error(
                                MessageType.CALL, requestId!!, emptyMap(),
                                testError, listOf(errorId)
                            )
                        }

                        logger.debug("Waiting for client to confirm call ${callId!!} got error=$errorId)")
                        deferredResults[callId!!].await() should be(errorId)
                    })
                }

                receiveJobs.forEach {
                    it.join()
                }
            }
        }
    }

    "Test client receiving a high volume of invocation messages and returning result" {
        ClientConversation {
            val calleeClient = newConnectedTestClient()

            val calleeProcedures = 100
            val invokeArgumentsKw = mapOf("one" to 1, "two" to "two")
            val callsToMakePerProcedure = 20

            runBlockingWithTimeout {
                repeat(calleeProcedures) { procedureIndex ->
                    var requestId: Long? = null
                    val testProcedure = Uri("callee.$procedureIndex")
                    launchWithTimeout {
                        calleeClient.register(testProcedure) { arguments, argumentsKw ->
                            CallResult(listOf(procedureIndex), argumentsKw)
                        }
                    }

                    launch(receiveContext) {
                        calleeClient shouldHaveSentMessage { message: Register ->
                            requestId = message.requestId
                        }

                        calleeClient willBeSentRouterMessage { Registered(requestId!!, procedureIndex.toLong()) }
                    }
                }
            }
            logger.debug("All registrations complete")

            repeat(callsToMakePerProcedure) { callId ->
                repeat(calleeProcedures) { procedureId ->
                    val callArg = callId * calleeProcedures + procedureId
                    launchWithTimeout {
                        calleeClient willBeSentRouterMessage {
                            Invocation(
                                callArg.toLong(),
                                procedureId.toLong(),
                                emptyMap(),
                                arguments = listOf(1),
                                argumentsKw = invokeArgumentsKw
                            )
                        }
                    }
                }
            }

            runBlockingWithTimeout {
                val receiveJobs = LinkedList<Job>()
                repeat(callsToMakePerProcedure) {
                    repeat(calleeProcedures) {
                        receiveJobs.add(launch(receiveContext) {
                            var requestId: Long?
                            calleeClient shouldHaveSentMessage { message: Yield ->
                                logger.debug("$message")
                                message.argumentsKw!! should containExactly<String, Any?>(invokeArgumentsKw)
                                requestId = message.requestId

                                logger.debug("Yield returned with requestID $requestId")
                            }
                        })
                    }
                }

                receiveJobs.forEach {
                    it.join()
                }
            }
        }
    }
})