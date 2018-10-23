package com.laurencegarmstrong.kwamp.client.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.ClientConversation
import com.laurencegarmstrong.kwamp.conversations.core.TestClient
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.WampClose
import com.laurencegarmstrong.kwamp.core.messages.Goodbye
import com.laurencegarmstrong.kwamp.core.messages.Hello
import com.laurencegarmstrong.kwamp.core.messages.Welcome
import io.kotlintest.be
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.runBlocking

class HelloAndGoodbye : StringSpec({
    "A client can say Hello and Goodbye" {
        ClientConversation {
            val client = TestClient()
            val testRealm = Uri("some.realm")

            val connectionJob = launchWithTimeout {
                client.connect(testRealm)
            }
            client shouldHaveSentMessage { message: Hello ->
                message.realm should be(testRealm)
            }
            client willBeSentRouterMessage { Welcome(123L, emptyMap()) }

            runBlocking {
                connectionJob.join()
            }

            val disconnectionJob = asyncWithTimeout {
                client.disconnect()
            }

            client shouldHaveSentMessage { message: Goodbye ->
                message.reason should be(WampClose.SYSTEM_SHUTDOWN.uri)
            }

            client willBeSentRouterMessage { Goodbye(emptyMap(), WampClose.GOODBYE_AND_OUT.uri) }

            runBlocking { disconnectionJob.await() } should be(WampClose.GOODBYE_AND_OUT.uri)
        }
    }
})