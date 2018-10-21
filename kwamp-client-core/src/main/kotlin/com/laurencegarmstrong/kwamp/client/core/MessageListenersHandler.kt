package com.laurencegarmstrong.kwamp.client.core

import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.messages.RequestMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class MessageListenersHandler {
    private val requestIdListeners =
        ConcurrentHashMap<Long, CompletableDeferred<Message>>()
    private val typeListeners =
        ConcurrentHashMap<KClass<out Message>, CompletableDeferred<Message>>()

    fun notifyListeners(message: Message) {
        if (message is RequestMessage) requestIdListeners.remove(message.requestId)?.complete(message)
        typeListeners.remove(message::class)?.complete(message)
    }

    inline fun <reified T : Message> registerListener() = registerTypeListener(T::class)

    fun <T : Message> registerTypeListener(messageType: KClass<T>): Deferred<T> = GlobalScope.async {
        registerToMessageListenerMap(typeListeners, messageType).await() as? T
            ?: throw IllegalStateException("not meant to happen")
    }

    fun <T : Message> registerListener(requestId: Long): Deferred<T> = GlobalScope.async {
        registerToMessageListenerMap(requestIdListeners, requestId).await() as? T
            ?: throw IllegalStateException("not meant to happen")
    }

    private fun <T> registerToMessageListenerMap(
        listenerMap: MutableMap<T, CompletableDeferred<Message>>,
        index: T
    ): CompletableDeferred<Message> {
        if (listenerMap.containsKey(index)) throw IllegalArgumentException("Already listening for $index")
        val completableDeferredMessage =
            CompletableDeferred<Message>()
        listenerMap[index] = completableDeferredMessage
        return completableDeferredMessage
    }
}