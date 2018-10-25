package com.laurencegarmstrong.kwamp.client.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.ClientConversation
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Publish
import com.laurencegarmstrong.kwamp.core.messages.Published
import io.kotlintest.be
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.containExactly
import io.kotlintest.matchers.haveKey
import io.kotlintest.should
import io.kotlintest.shouldNot
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.CompletableDeferred

class PublishAndSubscribe : StringSpec({
    "A Client publish an event unacknowledged" {
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

    "A Client publish an event and have it acknowledged" {
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
})