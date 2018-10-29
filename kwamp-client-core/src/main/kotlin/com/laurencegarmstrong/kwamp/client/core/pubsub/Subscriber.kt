package com.laurencegarmstrong.kwamp.client.core.pubsub

import com.laurencegarmstrong.kwamp.client.core.MessageListenersHandler
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.ProtocolViolationException
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.UriPattern
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

internal class Subscriber(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator,
    private val messageListenersHandler: MessageListenersHandler
) {
    private val subscriptions = ConcurrentHashMap<Long, EventHandler>()

    fun subscribe(
        topicPattern: UriPattern,
        eventHandler: EventHandler
    ) =
        runBlocking {
            val subscribed = createSubscription(topicPattern)
            subscriptions[subscribed.subscription] = eventHandler
            Any()
        }

    private suspend fun createSubscription(topicPattern: UriPattern): Subscribed {
        randomIdGenerator.newId().also { requestId ->
            connection.send(
                Subscribe(
                    requestId,
                    emptyMap(),
                    topicPattern
                )
            )
            return deferredSubscribedWithListeners(requestId).await()
        }
    }

    private fun deferredSubscribedWithListeners(requestId: Long): Deferred<Subscribed> =
        CompletableDeferred<Subscribed>().also {
            applyListenersToCompletableSubscribed(it, requestId)
        }

    private fun applyListenersToCompletableSubscribed(
        completableResult: CompletableDeferred<Subscribed>,
        requestId: Long
    ) = GlobalScope.launch {
        val deferredSubscribedMessage = messageListenersHandler.registerListener<Subscribed>(requestId)
        val deferredErrorMessage = messageListenersHandler.registerListener<Error>(requestId)
        launch {
            val registeredMessage = deferredSubscribedMessage.await()
            deferredErrorMessage.cancel()
            completableResult.complete(registeredMessage)
        }
        launch {
            val errorMessage = deferredErrorMessage.await()
            deferredSubscribedMessage.cancel()
            completableResult.cancel(/*error as exception*/)
        }
    }

    fun receiveEvent(eventMessage: Event) {
        //TODO use correct exception
        subscriptions[eventMessage.subscription]?.invoke(
            eventMessage.arguments,
            eventMessage.argumentsKw
        ) ?: throw ProtocolViolationException("No such subscription ${eventMessage.subscription}")
    }
}

typealias EventHandler = (arguments: List<Any?>?, argumentsKw: Dict?) -> Unit