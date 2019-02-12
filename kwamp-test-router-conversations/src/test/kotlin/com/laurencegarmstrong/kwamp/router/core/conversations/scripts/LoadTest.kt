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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LoadTest : StringSpec({
    "Test router sending and receiving a high volume of messages" {
        val numClients = 400;
        val clients = ArrayList<TestConnection>().apply {
            runBlocking {
                repeat(numClients) {
                    launch {
                        add(TestConnection())
                    }
                }
            }
        }

        val publisher = TestConnection()

        RouterConversation(
            defaultRouter(),
            *clients.toTypedArray(), publisher
        ) {
            runBlocking {
                clients.forEach { client ->
                    launch {
                        client.startsASession()
                    }
                }
                launch {
                    publisher.startsASession()
                }
            }

            val testTopic = Uri("test.topic")
            runBlocking {
                clients.forEachIndexed { index, client ->
                    launch {
                        client willSend { Subscribe(index.toLong(), emptyMap(), testTopic) }
                    }
                }
            }

            val clientSubscriptions = Array(clients.size) { index ->
                var subscriptionId: Long? = null
                clients[index] shouldReceiveMessage { message: Subscribed ->
                    message.requestId should be(index.toLong())
                    subscriptionId = message.subscription
                }
                subscriptionId
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

            runBlocking {
                clients.forEachIndexed { index, client ->
                    launch {
                        client shouldReceiveMessage eventMessageMatching(
                            { clientSubscriptions[index]!! },
                            { publicationId!! },
                            eventArgs,
                            eventArgsKw
                        )
                    }
                }
            }
        }
    }
})