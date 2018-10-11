package com.laurencegarmstrong.kwamp.router.core.conversations.scripts

import com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure.Conversation
import com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure.TestConnection
import com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure.defaultRouter
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.WampClose
import co.nz.arm.kwamp.core.messages.Goodbye
import co.nz.arm.kwamp.core.messages.Hello
import co.nz.arm.kwamp.core.messages.Welcome
import io.kotlintest.be
import io.kotlintest.matchers.haveKey
import io.kotlintest.matchers.haveKeys
import io.kotlintest.should
import io.kotlintest.specs.StringSpec

class HelloGoodbye : StringSpec({
    "Test Hello and Goodbye" {
        val client = TestConnection()
        Conversation(
            defaultRouter(),
            client
        ) {
            client willSend { Hello(Uri("default"), emptyMap()) }
            client shouldReceiveMessage { message: Welcome ->
                message.details should haveKey("agent")
                message.details should haveKey("roles")
                message.details should haveKey("roles")
                asDict(message.details["roles"]) should haveKeys("broker", "dealer")
            }

            client willSend { Goodbye(emptyMap(), WampClose.SYSTEM_SHUTDOWN.uri) }
            client shouldReceiveMessage { message: Goodbye ->
                message.reason should be(WampClose.GOODBYE_AND_OUT.uri)
            }
        }
    }
})
