package com.laurencegarmstrong.kwamp.client.core.conversations.scripts

import com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure.ClientConversation
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.UriPattern
import com.laurencegarmstrong.kwamp.core.WampError
import com.laurencegarmstrong.kwamp.core.WampErrorException
import com.laurencegarmstrong.kwamp.core.messages.*
import io.kotlintest.*
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.containExactly
import io.kotlintest.matchers.haveKey
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.CompletableDeferred

class PublishAndSubscribe : StringSpec({
    "A Client can publish an event unacknowledged" {
        ClientConversation {
            val client = newConnectedTestClient()
            val testTopic = Uri("test.topic")
            val publishArguments = listOf(1, 2, "three")
            val publishArgumentsKw = mapOf("one" to 1, "two" to "two")
            launchWithTimeout {
                client.publish(testTopic, publishArguments, publishArgumentsKw)
            }

            client shouldHaveSentMessage { message: Publish ->
                message.topic should be(testTopic)
                message.arguments shouldContainExactly publishArguments
                message.argumentsKw!! should containExactly<String, Any?>(publishArgumentsKw)
                message.options shouldNot haveKey("acknowledge")
            }
        }
    }

    "A Client can publish an event and have it acknowledged" {
        ClientConversation {
            val client = newConnectedTestClient()
            val testTopic = Uri("test.topic")
            val publishArguments = listOf(1, 2, "three")
            val publishArgumentsKw = mapOf("one" to 1, "two" to "two")

            val deferredPublicationId = CompletableDeferred<Long>()
            launchWithTimeout {
                client.publish(testTopic, publishArguments, publishArgumentsKw) { id ->
                    deferredPublicationId.complete(id)
                }
            }

            var requestId: Long? = null
            client shouldHaveSentMessage { message: Publish ->
                message.topic should be(testTopic)
                message.arguments shouldContainExactly publishArguments
                message.argumentsKw!! should containExactly<String, Any?>(publishArgumentsKw)
                message.options should haveKey("acknowledge")
                requestId = message.requestId
            }

            val publicationId = 123L
            client willBeSentRouterMessage { Published(requestId!!, publicationId) }

            runBlockingWithTimeout {
                deferredPublicationId.await() should be(publicationId)
            }
        }
    }

    "A Client subscribe to a topic and receive events" {
        ClientConversation {
            val client = newConnectedTestClient()

            val numOfEvents = 2
            val completions = mutableListOf<CompletableDeferred<Int>>()
            for (i in 0..numOfEvents) {
                completions.add(CompletableDeferred())
            }

            val testSubUriPattern = UriPattern("test.sub")
            launchWithTimeout {
                client.subscribe(testSubUriPattern) { arguments, _ ->
                    val index = arguments!![0] as Int
                    completions[index].complete(index)
                }
            }

            var requestId: Long? = null
            client shouldHaveSentMessage { message: Subscribe ->
                message.topic should be(testSubUriPattern)
                requestId = message.requestId
            }

            client willBeSentRouterMessage { Subscribed(requestId!!, 123L) }

            val eventArgumentsKw = mapOf("one" to 1, "two" to "two")
            for (i in 0..numOfEvents) {
                client willBeSentRouterMessage {
                    Event(123L, i.toLong(), emptyMap(), listOf(i.toLong()), eventArgumentsKw)
                }
            }

            for (i in 0..numOfEvents) {
                runBlockingWithTimeout {
                    completions[i].await() should be(i)
                }
            }
        }
    }

    "Client receives an error when trying to subscribe" {
        ClientConversation {
            val client = newConnectedTestClient()


            val testSubUriPattern = UriPattern("test.sub")
            val deferredError = asyncWithTimeout {
                try {
                    client.subscribe(testSubUriPattern) { _, _ -> }
                    null
                } catch (error: WampErrorException) {
                    error
                }
            }

            var requestId: Long? = null
            client shouldHaveSentMessage { message: Subscribe ->
                message.topic should be(testSubUriPattern)
                requestId = message.requestId
            }

            val errorArguments = listOf(1, 2, "three")
            val errorArgumentsKw = mapOf("one" to 1, "two" to "two")
            client willBeSentRouterMessage {
                Error(
                    MessageType.SUBSCRIBE,
                    requestId!!,
                    emptyMap(),
                    WampError.NO_SUCH_SUBSCRIPTION.uri,
                    errorArguments,
                    errorArgumentsKw
                )
            }

            runBlockingWithTimeout {
                val error = deferredError.await()
                error shouldNotBe null
                error!!.requestType should be(MessageType.SUBSCRIBE)
                error.requestId should be(requestId!!)
                error.arguments shouldContainExactly errorArguments
                error.argumentsKw!! should containExactly<String, Any?>(errorArgumentsKw)
            }
        }
    }
})