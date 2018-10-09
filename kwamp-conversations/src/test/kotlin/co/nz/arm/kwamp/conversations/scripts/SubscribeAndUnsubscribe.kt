package co.nz.arm.kwamp.conversations.scripts

import co.nz.arm.kwamp.conversations.infrastructure.Conversation
import co.nz.arm.kwamp.conversations.infrastructure.TestConnection
import co.nz.arm.kwamp.conversations.infrastructure.defaultRouter
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.messages.Subscribe
import co.nz.arm.kwamp.core.messages.Subscribed
import co.nz.arm.kwamp.core.messages.Unsubscribe
import co.nz.arm.kwamp.core.messages.Unsubscribed
import co.nz.arm.kwamp.router.Router
import io.kotlintest.be
import io.kotlintest.should
import io.kotlintest.specs.StringSpec

class SubUnSub : StringSpec({
    "Test Subscribing and unsubscribing" {
        subscribeAndUnsubscribe().execute()
    }
})

fun subscribeAndUnsubscribe(
    router: Router = defaultRouter(),
    client: TestConnection = TestConnection()
) =
    Conversation(router, client) {
        client.startsASession()

        var subscription: Long? = null
        client willSend { Subscribe(123L, emptyMap(), Uri("sub.topic")) }
        client shouldReceiveMessage { message: Subscribed ->
            message.requestId should be(123L)
            subscription = message.subscription
        }

        client willSend { Unsubscribe(456L, subscription!!) }
        client shouldReceiveMessage { message: Unsubscribed ->
            message.requestId should be(456L)
        }
    }

