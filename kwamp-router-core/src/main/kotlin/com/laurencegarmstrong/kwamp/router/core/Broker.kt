package com.laurencegarmstrong.kwamp.router.core

import com.laurencegarmstrong.kwamp.core.*
import com.laurencegarmstrong.kwamp.core.messages.Publish
import com.laurencegarmstrong.kwamp.core.messages.Subscribe
import com.laurencegarmstrong.kwamp.core.messages.Unsubscribe
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

const val ACKNOWLEDGE_OPTION = "acknowledge"

class Broker(private val messageSender: MessageSender, private val randomIdGenerator: RandomIdGenerator) {
    private val subscriptionLock = ReentrantLock()
    private val topicSubscriptions = HashMap<UriPattern, MutableList<Long>>()
    private val subscriptions = IdentifiableSet<Subscription>(randomIdGenerator)
    private val sessionSubscriptions = ConcurrentHashMap<Long, MutableSet<Long>>()

    fun subscribe(session: WampSession, subscriptionMessage: Subscribe) {
        val subscriptionId = findExistingSubscription(session, subscriptionMessage.topic)
            ?: newSubscription(
                session,
                subscriptionMessage
            ).subscriptionId

        messageSender.sendSubscribed(session.connection, subscriptionMessage.requestId, subscriptionId)
    }

    private fun findExistingSubscription(subscriberSession: WampSession, topic: UriPattern) =
        subscriptionLock.withLock {
            topicSubscriptions[topic]?.find { subscriptions[it]!!.session == subscriberSession }
        }

    private fun newSubscription(session: WampSession, subscriptionMessage: Subscribe) =
    //TODO should new ID be random?
        subscriptionLock.withLock {
            subscriptions.putWithId { subscriptionId ->
                sessionSubscriptions.computeIfAbsent(session.id) { hashSetOf() }.add(subscriptionId)
                topicSubscriptions.computeIfAbsent(subscriptionMessage.topic) { mutableListOf() } += subscriptionId
                Subscription(subscriptionMessage.topic, session, subscriptionId)
            }
        }

    fun unsubscribe(session: WampSession, unsubscribeMessage: Unsubscribe) {
        sessionSubscriptions[session.id]?.remove(unsubscribeMessage.subscription)
        try {
            removeSubscription(unsubscribeMessage.subscription)
        } catch (error: NoSuchSubscriptionException) {
            throw NoSuchSubscriptionErrorException(unsubscribeMessage.requestId)
        }

        messageSender.sendUnsubscribe(session.connection, unsubscribeMessage.requestId)
    }

    private fun removeSubscription(subscriptionId: Long) {
        subscriptionLock.withLock {
            val subscription = subscriptions.remove(subscriptionId)
                ?: throw NoSuchSubscriptionException()

            topicSubscriptions[subscription.topic]!!.removeIf(subscriptionId::equals)
        }
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

    fun cleanSessionResources(sessionId: Long) {
        sessionSubscriptions.remove(sessionId)?.forEach { subscriptionId ->
            removeSubscription(subscriptionId)
        }
    }
}

class NoSuchSubscriptionException : IllegalArgumentException()

data class Subscription(
    val topic: UriPattern,
    val session: WampSession,
    val subscriptionId: Long
)
