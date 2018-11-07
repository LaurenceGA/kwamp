package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.Publish
import com.laurencegarmstrong.kwamp.core.messages.Subscribe
import com.laurencegarmstrong.kwamp.core.messages.Unsubscribe
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

const val ACKNOWLEDGE_OPTION = "acknowledge"

class Broker(private val messageSender: MessageSender, private val randomIdGenerator: RandomIdGenerator) {
    private val subscriptionLock = ReentrantLock()
    private val topicSubscriptions = HashMap<UriPattern, MutableList<Long>>()
    private val subscriptions = IdentifiableSet<Subscription>(randomIdGenerator)

    fun subscribe(session: WampSession, subscriptionMessage: Subscribe) {
        val subscriptionId = findExistingSubscription(session, subscriptionMessage.topic)
            ?: newSubscription(
                session,
                subscriptionMessage
            ).subscriptionId

        messageSender.sendSubscribed(session.connection, subscriptionMessage.requestId, subscriptionId)
    }

    private fun findExistingSubscription(subscriberSession: WampSession, topic: UriPattern) =
        topicSubscriptions[topic]?.find { subscriptions[it]!!.session == subscriberSession }

    private fun newSubscription(session: WampSession, subscriptionMessage: Subscribe) =
    //TODO should new ID be random?
        subscriptionLock.withLock {
            subscriptions.putWithId { subscriptionId ->
                topicSubscriptions.computeIfAbsent(subscriptionMessage.topic) { mutableListOf() } += subscriptionId
                Subscription(subscriptionMessage.topic, session, subscriptionId)
            }
        }

    fun unsubscribe(session: WampSession, unsubscribeMessage: Unsubscribe) {
        subscriptionLock.withLock {
            val subscription = subscriptions.remove(unsubscribeMessage.subscription)
                ?: throw NoSuchSubscriptionException(unsubscribeMessage.requestId)

            topicSubscriptions[subscription.topic]!!.removeIf(unsubscribeMessage.subscription::equals)
        }

        messageSender.sendUnsubscribe(session.connection, unsubscribeMessage.requestId)
    }

    fun publish(session: WampSession, publishMessage: Publish) {
        val publicationId = randomIdGenerator.newId()
        getSubscriptions(session, publishMessage.topic).forEach { subscription ->
            messageSender.sendEvent(
                subscription.session.connection,
                subscription.subscriptionId,
                publicationId,
                publishMessage.arguments,
                publishMessage.argumentsKw
            )
        }

        if (shouldBeAcknowledged(publishMessage)) {
            messageSender.sendPublished(session.connection, publishMessage.requestId, publicationId)
        }
    }

    private fun shouldBeAcknowledged(publishMessage: Publish) =
        publishMessage.options.getOrDefault(ACKNOWLEDGE_OPTION, false) as? Boolean ?: false

    private fun getSubscriptions(publisherSession: WampSession, topic: Uri) =
        subscriptionLock.withLock {
            getTopicSubscriptions(topic).filter { it.session != publisherSession }
        }

    private fun getTopicSubscriptions(topic: Uri) =
        topicSubscriptions[topic]?.map { subscriptionId ->
            subscriptions[subscriptionId]!!
        } ?: emptyList()
}

data class Subscription(
    val topic: UriPattern,
    val session: WampSession,
    val subscriptionId: Long
)
