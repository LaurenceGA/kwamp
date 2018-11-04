package com.laurencegarmstrong.kwamp.router.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.conversations.core.defaultRouter
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.WampClose
import com.laurencegarmstrong.kwamp.core.messages.Goodbye
import com.laurencegarmstrong.kwamp.core.messages.Hello
import com.laurencegarmstrong.kwamp.core.messages.Welcome
import com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure.RouterConversation
import io.kotlintest.be
import io.kotlintest.matchers.haveKey
import io.kotlintest.matchers.haveKeys
import io.kotlintest.should
import io.kotlintest.specs.StringSpec

class HelloGoodbye : StringSpec({
    "A router can receive and respond to a client's Hello and Goodbye" {
        val client = TestConnection()
        RouterConversation(
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
