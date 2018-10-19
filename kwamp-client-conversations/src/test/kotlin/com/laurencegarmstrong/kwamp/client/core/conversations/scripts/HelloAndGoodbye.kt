package com.laurencegarmstrong.kwamp.client.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.ClientConversation
import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.core.messages.Hello
import com.laurencegarmstrong.kwamp.core.messages.Welcome
import io.kotlintest.be
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HelloAndGoodbye : StringSpec({
    "A client can say Hello and Goodbye" {
        ClientConversation(TestConnection()) {
            GlobalScope.launch {
                val client = newTestClient()
            }
            router shouldReceiveMessage { message: Hello ->
                message.realm should be(realm)
            }

            router willSend { Welcome(123L, emptyMap()) }
        }
    }
})