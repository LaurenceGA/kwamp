package com.laurencegarmstrong.kwamp.client.core.pubsub

import com.laurencegarmstrong.kwamp.client.core.MessageListenersHandler
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.ProtocolViolationException
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.UriPattern
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.runBlocking
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
    ): SubscriptionHandle =
        runBlocking {
            val subscribed = createSubscription(topicPattern)
            subscriptions[subscribed.subscription] = eventHandler
            SubscriptionHandle { unsubscribe(subscribed.subscription) }
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

            return messageListenersHandler.registerListenerWithErrorHandler<Subscribed>(requestId).await()
        }
    }

    private fun unsubscribe(subscriptionId: Long) {
        runBlocking {
            performUnsubscribe(subscriptionId)
            subscriptions.remove(subscriptionId)
        }
    }

    private suspend fun performUnsubscribe(subscriptionId: Long) {
        randomIdGenerator.newId().also { requestId ->
            connection.send(
                Unsubscribe(
                    requestId,
                    subscriptionId
                )
            )
            messageListenersHandler.registerListenerWithErrorHandler<Unsubscribed>(requestId).await()
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

class SubscriptionHandle(private val unsubscribeCallback: () -> Unit) {
    fun unsubscribe() = unsubscribeCallback()
}