package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.NoSuchSubscriptionException
import co.nz.arm.kwamp.core.RandomIdGenerator
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.messages.Subscribe
import co.nz.arm.kwamp.core.messages.Unsubscribe
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SubscriptionHandler(private val messageSender: MessageSender, private val randomIdGenerator: RandomIdGenerator) {
    private val subscriptionLock = ReentrantLock()
    private val topicSubscriptions = HashMap<Uri, MutableList<Long>>()
    private val subscriptions = HashMap<Long, Subscription>()

    fun subscribe(session: WampSession, subscriptionMessage: Subscribe) {
        val subscriptionId = findExistingSubscription(session, subscriptionMessage.topic)
            ?: newSubscription(
                session,
                subscriptionMessage
            ).subscriptionId

        messageSender.sendSubscribed(session.connection, subscriptionMessage.requestId, subscriptionId)
    }

    private fun findExistingSubscription(subscriberSession: WampSession, topic: Uri) =
        topicSubscriptions[topic]?.find { subscriptions[it]!!.session == subscriberSession }

    private fun newSubscription(session: WampSession, subscriptionMessage: Subscribe) =
    //TODO should new ID be random?
        randomIdGenerator.newId().let { subscriptionId ->
            Subscription(subscriptionMessage.topic, session, subscriptionId)
                .also { subscription ->
                    subscriptionLock.withLock {
                        topicSubscriptions.computeIfAbsent(subscriptionMessage.topic) { mutableListOf() } += subscriptionId
                        subscriptions[subscriptionId] = subscription
                    }
                }
        }

    fun unsubscribe(session: WampSession, unsubscribeMessage: Unsubscribe) {
        subscriptionLock.withLock {
            val subscription = subscriptions.remove(unsubscribeMessage.subscription)
                ?: throw NoSuchSubscriptionException(unsubscribeMessage.requestId)

            //TODO release ID after use from random id generator
            topicSubscriptions[subscription.topic]?.removeIf(unsubscribeMessage.subscription::equals)
                ?: throw IllegalStateException("Couldn't find subscription URI")
        }

        messageSender.sendUnsubscribe(session.connection, unsubscribeMessage.requestId)
    }
}

data class Subscription(
    val topic: Uri,
    val session: WampSession,
    val subscriptionId: Long
)
