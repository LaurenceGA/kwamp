package com.laurencegarmstrong.kwamp.router.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.conversations.core.defaultRouter
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.WampClose
import com.laurencegarmstrong.kwamp.core.messages.*
import com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure.RouterConversation
import io.kotlintest.be
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.containExactly
import io.kotlintest.should
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

class Rpc : StringSpec({
    "Test Remote Procedure Call" {
        val clientA = TestConnection()
        val clientB = TestConnection()
        RouterConversation(
            defaultRouter(),
            clientA,
            clientB
        ) {
            clientA.startsASession()
            clientB.startsASession()

            clientA willSend {
                Register(
                    123L,
                    emptyMap(),
                    Uri("clientA.proc")
                )
            }
            clientA shouldReceiveMessage { message: Registered ->
                message.requestId should be(123L)
                message.registration should be(1L)
            }

            clientB willSend {
                Call(
                    456L,
                    emptyMap(),
                    Uri("clientA.proc"),
                    listOf("arg1", 2),
                    mapOf("hello" to 3)
                )
            }
            var requestId: Long? = null
            clientA shouldReceiveMessage { message: Invocation ->
                requestId = message.requestId
                message.registration should be(1L)
                message.arguments shouldNotBe null
                message.arguments shouldContainExactly listOf("arg1", 2)
                message.argumentsKw shouldNotBe null
                message.argumentsKw!! should containExactly<String, Any?>(mapOf("hello" to 3))
            }

            clientA willSend {
                Yield(
                    requestId!!,
                    emptyMap(),
                    listOf("result1", 4),
                    mapOf("world" to 5)
                )
            }
            clientB shouldReceiveMessage { message: Result ->
                message.requestId should be(456L)
                message.arguments shouldNotBe null
                message.arguments shouldContainExactly listOf("result1", 4)
                message.argumentsKw shouldNotBe null
                message.argumentsKw!! should containExactly<String, Any?>(mapOf("world" to 5))
            }
        }
    }

    "Remove procedure registrations on disconnect" {
        val clientA = TestConnection()
        val clientB = TestConnection()
        RouterConversation(
            defaultRouter(),
            clientA,
            clientB
        ) {
            clientA.startsASession()
            clientB.startsASession()

            clientA willSend {
                Register(
                    123L,
                    emptyMap(),
                    Uri("clientA.proc")
                )
            }
            clientA shouldReceiveMessage { message: Registered ->
                message.requestId should be(123L)
                message.registration should be(1L)
            }

            clientA willSend {
                Goodbye(emptyMap(), WampClose.GOODBYE_AND_OUT.uri)
            }

            clientA shouldReceiveMessage { message: Goodbye ->
                message.reason should be(WampClose.GOODBYE_AND_OUT.uri)
            }

            clientB willSend {
                Register(
                    123L,
                    emptyMap(),
                    Uri("clientA.proc")
                )
            }
            clientB shouldReceiveMessage { message: Registered ->
                message.requestId should be(123L)
                message.registration should be(1L)
            }
        }
    }
})


