package com.laurencegarmstrong.kwamp.router.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.conversations.core.defaultRouter
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Publish
import com.laurencegarmstrong.kwamp.core.messages.Published
import com.laurencegarmstrong.kwamp.core.messages.Subscribe
import com.laurencegarmstrong.kwamp.core.messages.Subscribed
import com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure.RouterConversation
import io.kotlintest.be
import io.kotlintest.should
import io.kotlintest.specs.StringSpec

class LoadTest : StringSpec({
    "Test router sending a high volume of messages" {
        val numClients = 5;
        val clients = ArrayList<TestConnection>().apply {
            repeat(numClients) {
                add(TestConnection())
            }
        }

        val publisher = TestConnection()

        RouterConversation(
            defaultRouter(),
            *clients.toTypedArray()
        ) {
            clients.forEach { client ->
                client.startsASession()
            }
            publisher.startsASession()

            val testTopic = Uri("test.topic")
            clients.forEachIndexed { index, client ->
                client willSend { Subscribe(index.toLong(), emptyMap(), testTopic) }
            }

            val clientSubscriptions = ArrayList<Long?>(clients.size);
            clients.forEachIndexed { index, client ->
                client shouldReceiveMessage { message: Subscribed ->
                    message.requestId should be(index.toLong())
                    clientSubscriptions[index] = message.subscription
                }
            }

            val eventArgs = listOf(1, 2, "three")
            val eventArgsKw = mapOf("one" to 1, "two" to 2)
            publisher willSend {
                Publish(
                    123L,
                    mapOf("acknowledge" to true),
                    testTopic,
                    eventArgs,
                    eventArgsKw
                )
            }
            var publicationId: Long? = null
            publisher shouldReceiveMessage { message: Published ->
                message.requestId should be(123L)
                publicationId = message.publication
            }

            clients.forEachIndexed { index, client ->
                client shouldReceiveMessage eventMessage(
                    { clientSubscriptions[index]!! },
                    { publicationId!! },
                    eventArgs,
                    eventArgsKw
                )
            }
        }
    }
})

//internal fun eventMessage(
//    subscription: () -> Long,
//    publication: () -> Long,
//    arguments: List<Any?>,
//    argumentsKw: Map<String, Any?>
//) =
//    { message: Event ->
//        message.subscription should be(subscription())
//        message.publication should be(publication())
//        message.arguments!!.shouldContainExactly(*arguments.toTypedArray())
//        message.argumentsKw!!.shouldContainExactly(argumentsKw)
//    }