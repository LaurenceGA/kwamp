package co.nz.arm.kwamp.conversations.scripts

import co.nz.arm.kwamp.conversations.infrastructure.Conversation
import co.nz.arm.kwamp.conversations.infrastructure.TestConnection
import co.nz.arm.kwamp.conversations.infrastructure.defaultRouter
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.messages.*
import co.nz.arm.kwamp.router.Router
import io.kotlintest.be
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.maps.shouldContainExactly
import io.kotlintest.should
import io.kotlintest.specs.StringSpec

class PublishEvent : StringSpec({
    "Test publishing an event" {
        publishEvent().execute()
    }
})

fun publishEvent(
    router: Router = defaultRouter(),
    clientA: TestConnection = TestConnection(),
    clientB: TestConnection = TestConnection(),
    clientC: TestConnection = TestConnection(),
    clientD: TestConnection = TestConnection()
) =
    Conversation(router, clientA, clientB, clientC, clientD) {
        clientA.startsASession()
        clientB.startsASession()
        clientC.startsASession()
        clientD.startsASession()

        val testTopic = Uri("test.topic")
        clientB willSend { Subscribe(1L, emptyMap(), testTopic) }
        clientC willSend { Subscribe(2L, emptyMap(), testTopic) }
        clientD willSend { Subscribe(3L, emptyMap(), testTopic) }

        var clientBSubscription: Long? = null
        clientB shouldReceiveMessage { message: Subscribed ->
            message.requestId should be(1L)
            clientBSubscription = message.subscription
        }
        var clientCSubscription: Long? = null
        clientC shouldReceiveMessage { message: Subscribed ->
            message.requestId should be(2L)
            clientCSubscription = message.subscription
        }
        var clientDSubscription: Long? = null
        clientD shouldReceiveMessage { message: Subscribed ->
            message.requestId should be(3L)
            clientDSubscription = message.subscription
        }

        val eventArgs = listOf(1, 2, "three")
        val eventArgsKw = mapOf("one" to 1, "two" to 2)
        clientA willSend { Publish(123L, mapOf("acknowledged" to true), testTopic, eventArgs, eventArgsKw) }
        var publicationId: Long? = null
        clientA shouldReceiveMessage { message: Published ->
            message.requestId should be(123L)
            publicationId = message.publication
        }

        clientB shouldReceiveMessage eventMessage(
            { clientBSubscription!! },
            { publicationId!! },
            eventArgs,
            eventArgsKw
        )
        clientC shouldReceiveMessage eventMessage(
            { clientCSubscription!! },
            { publicationId!! },
            eventArgs,
            eventArgsKw
        )
        clientD shouldReceiveMessage eventMessage(
            { clientDSubscription!! },
            { publicationId!! },
            eventArgs,
            eventArgsKw
        )
    }

internal fun eventMessage(
    subscription: () -> Long,
    publication: () -> Long,
    arguments: List<Any?>,
    argumentsKw: Map<String, Any?>
) =
    { message: Event ->
        message.subscription should be(subscription())
        message.publication should be(publication())
        message.arguments!!.shouldContainExactly(*arguments.toTypedArray())
        message.argumentsKw!!.shouldContainExactly(argumentsKw)
    }