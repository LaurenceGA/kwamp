package com.laurencegarmstrong.kwamp.router.core.conversations.infrastructure

import com.laurencegarmstrong.kwamp.conversations.core.ConversationCanvas
import com.laurencegarmstrong.kwamp.conversations.core.TestConnection
import com.laurencegarmstrong.kwamp.router.core.Router
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RouterConversation(
    router: Router,
    vararg clients: TestConnection,
    conversationDefinition: ConversationCanvas.() -> Unit
) {
    init {
        runBlocking {
            clients.forEach { testConnection ->
                launch {
                    router.registerConnection(testConnection.connection)
                }
            }
        }
        ConversationCanvas().conversationDefinition()
    }
}