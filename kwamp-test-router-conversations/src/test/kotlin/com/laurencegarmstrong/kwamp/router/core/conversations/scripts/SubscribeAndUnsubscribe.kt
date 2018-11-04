package com.laurencegarmstrong.kwamp.router.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.conversations.core.defaultRouter
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Subscribe
import com.laurencegarmstrong.kwamp.core.messages.Subscribed
import com.laurencegarmstrong.kwamp.core.messages.Unsubscribe
import com.laurencegarmstrong.kwamp.core.messages.Unsubscribed
import com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure.RouterConversation
import io.kotlintest.be
import io.kotlintest.should
import io.kotlintest.specs.StringSpec

class SubUnSub : StringSpec({
    "Test Subscribing and unsubscribing" {
        val client = TestConnection()
        RouterConversation(
            defaultRouter(),
            client
        ) {
            client.startsASession()

            var subscription: Long? = null
            client willSend {
                Subscribe(
                    123L,
                    emptyMap(),
                    Uri("sub.topic")
                )
            }
            client shouldReceiveMessage { message: Subscribed ->
                message.requestId should be(123L)
                subscription = message.subscription
            }

            client willSend { Unsubscribe(456L, subscription!!) }
            client shouldReceiveMessage { message: Unsubscribed ->
                message.requestId should be(456L)
            }
        }
    }
})

