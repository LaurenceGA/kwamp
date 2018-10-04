package co.nz.arm.kwamp.router

import co.nz.arm.kwamp.core.Connection
import co.nz.arm.kwamp.core.NoSuchSubscriptionException
import co.nz.arm.kwamp.core.RandomIdGenerator
import co.nz.arm.kwamp.core.Uri
import co.nz.arm.kwamp.core.messages.Subscribe
import co.nz.arm.kwamp.core.messages.Unsubscribe
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SubscriptionHandler(private val messageSender: MessageSender, private val randomIdGenerator: RandomIdGenerator) {
    private val subscriptionLock = ReentrantLock()
    private val subscriptionTopics = HashMap<Uri, Long>()
    private val subscriptions = HashMap<Long, Subscription>()

    fun subscribe(connection: Connection, subscriptionMessage: Subscribe) {
        //TODO should this be random?
        randomIdGenerator.newId().let { subscriptionId ->
            subscriptionLock.withLock {
                subscriptionTopics[subscriptionMessage.topic] = subscriptionId
                subscriptions[subscriptionId] = Subscription(subscriptionMessage.topic, connection, subscriptionId)
            }
            messageSender.sendSubscribed(connection, subscriptionMessage.requestId, subscriptionId)
        }
    }

    fun unsubscribe(connection: Connection, unsubscribeMessage: Unsubscribe) {
        subscriptionLock.withLock {
            val subscription = subscriptions.remove(unsubscribeMessage.subscription)
                ?: throw NoSuchSubscriptionException(unsubscribeMessage.requestId)

            //TODO release ID after use from random id generator
            subscriptionTopics.remove(subscription.topic)
                ?: throw IllegalStateException("Couldn't find subscription URI")
        }
        messageSender.sendUnsubscribe(connection, unsubscribeMessage.requestId)
    }

}

data class Subscription(
    val topic: Uri,
    val connection: Connection,
    val subscriptionId: Long
)
