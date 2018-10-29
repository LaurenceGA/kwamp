package com.laurencegarmstrong.kwamp.client.core.pubsub

import com.laurencegarmstrong.kwamp.client.core.MessageListenersHandler
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Dict
import com.laurencegarmstrong.kwamp.core.messages.Publish
import com.laurencegarmstrong.kwamp.core.messages.Published
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class Publisher(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator,
    private val messageListenersHandler: MessageListenersHandler
) {
    fun publish(topic: Uri, arguments: List<Any?>?, argumentsKw: Dict?, onPublished: ((Long) -> Unit)?) {
        runBlocking {
            randomIdGenerator.newId().also { requestId ->
                val optionsMap = if (onPublished != null) mapOf("acknowledge" to true) else emptyMap()
                connection.send(Publish(requestId, optionsMap, topic, arguments, argumentsKw))

                if (onPublished != null) {
                    launch {
                        val published =
                            messageListenersHandler.registerListener<Published>(requestId).await()
                        onPublished(published.publication)
                    }
                }
            }
        }
    }
}