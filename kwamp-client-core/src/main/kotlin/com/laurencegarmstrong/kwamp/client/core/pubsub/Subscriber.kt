package com.laurencegarmstrong.kwamp.client.core.pubsub

import com.laurencegarmstrong.kwamp.client.core.MessageListenersHandler
import com.laurencegarmstrong.kwamp.core.Connection
import com.laurencegarmstrong.kwamp.core.RandomIdGenerator
import com.laurencegarmstrong.kwamp.core.UriPattern
import com.laurencegarmstrong.kwamp.core.messages.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

internal class Subscriber(
    private val connection: Connection,
    private val randomIdGenerator: RandomIdGenerator,
    private val messageListenersHandler: MessageListenersHandler
) {
    private val subscriptions = ConcurrentHashMap<Long, EventHandler>()

    private val logger = LoggerFactory.getLogger(Subscriber::class.java)!!

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
            val messageListener = messageListenersHandler.registerListenerWithErrorHandler<Subscribed>(requestId)

            connection.send(
                Subscribe(
                    requestId,
                    emptyMap(),
                    topicPattern
                )
            )

            return messageListener.await()
        }
    }

    private fun unsubscribe(subscriptionId: Long) {
        runBlocking {
            subscriptions.remove(subscriptionId)
            unsubscribeFromRouter(subscriptionId)
        }
    }

    private suspend fun unsubscribeFromRouter(subscriptionId: Long) {
        randomIdGenerator.newId().also { requestId ->
            val messageListener = messageListenersHandler.registerListenerWithErrorHandler<Unsubscribed>(requestId)

            connection.send(
                Unsubscribe(
                    requestId,
                    subscriptionId
                )
            )
            messageListener.await()
        }
    }

    fun receiveEvent(eventMessage: Event) {
        subscriptions[eventMessage.subscription]?.invoke(
            eventMessage.arguments,
            eventMessage.argumentsKw
        )
            ?: logger.warn("Got an event (${eventMessage.publication}) for a subscription we don't have (${eventMessage.subscription})")
    }
}

typealias EventHandler = (arguments: List<Any?>?, argumentsKw: Dict?) -> Unit

class SubscriptionHandle(private val unsubscribeCallback: () -> Unit) {
    fun unsubscribe() = unsubscribeCallback()
}