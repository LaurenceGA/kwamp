package com.laurencegarmstrong.kwamp.client.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.ClientConversation
import com.laurencegarmstrong.kwamp.conversations.core.RECEIVE_TIMEOUT
import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.core.WampClose
import com.laurencegarmstrong.kwamp.core.messages.Goodbye
import com.laurencegarmstrong.kwamp.core.messages.Hello
import com.laurencegarmstrong.kwamp.core.messages.Welcome
import io.kotlintest.be
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class HelloAndGoodbye : StringSpec({
    "A client can say Hello and Goodbye" {
        ClientConversation(TestConnection()) {
            val createClientJob = async {
                //TODO push wait down a level to helper function
                withTimeout(RECEIVE_TIMEOUT) {
                    newTestClient()
                }
            }
            router shouldReceiveMessage { message: Hello ->
                message.realm should be(realm)
            }

            router willSend { Welcome(123L, emptyMap()) }

            val client = runBlocking {
                createClientJob.await()
            }

            val disconnectJob = async {
                withTimeout(RECEIVE_TIMEOUT) {
                    client.disconnect()
                }
            }

            router shouldReceiveMessage { message: Goodbye ->
                message.reason should be(WampClose.SYSTEM_SHUTDOWN.uri)
            }

            router willSend { Goodbye(emptyMap(), WampClose.GOODBYE_AND_OUT.uri) }

            runBlocking {
                disconnectJob.await()
            }
        }
    }
})