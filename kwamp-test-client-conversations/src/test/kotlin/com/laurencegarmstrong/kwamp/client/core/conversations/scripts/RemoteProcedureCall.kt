package com.laurencegarmstrong.kwamp.client.core.conversations.scripts

import com.laurencegarmstrong.kwamp.conversations.core.ClientConversation
import com.laurencegarmstrong.kwamp.conversations.core.RECEIVE_TIMEOUT
import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

class RemoteProcedureCall : StringSpec({
    "A client can call the procedure of another client" {
        ClientConversation(TestConnection()) {
            val createClientAJob = async {
                withTimeout(RECEIVE_TIMEOUT) {
                    newTestClient()
                }
            }
            val createClientBJob = async {
                withTimeout(RECEIVE_TIMEOUT) {
                    newTestClient()
                }
            }
        }
    }
})