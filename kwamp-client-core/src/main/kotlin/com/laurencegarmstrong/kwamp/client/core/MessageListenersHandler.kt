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
        ConcurrentHashMap<RequestListenerKey, CompletableDeferred<Message>>()
    private val typeListeners =
        ConcurrentHashMap<KClass<out Message>, CompletableDeferred<Message>>()

    fun notifyListeners(message: Message) {
        if (message is RequestMessage)
            requestIdListeners.remove(RequestListenerKey(message.requestId, message::class))?.complete(message)
        typeListeners.remove(message::class)?.complete(message)
    }

    inline fun <reified T : Message> registerListener() = registerTypeListener(T::class)

    inline fun <reified T : Message> registerListener(requestId: Long): Deferred<T> =
        registerListener(requestId, T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Message> registerTypeListener(messageType: KClass<out T>): Deferred<T> = GlobalScope.async {
        registerToMessageListenerMap(typeListeners, messageType).await() as T
    }.apply {
        invokeOnCompletion {
            typeListeners.remove(messageType)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Message> registerListener(requestId: Long, messageType: KClass<out T>): Deferred<T> =
        GlobalScope.async {
            registerToMessageListenerMap(
                requestIdListeners,
                RequestListenerKey(requestId, messageType)
            ).await() as T
        }.apply {
            invokeOnCompletion {
                requestIdListeners.remove(RequestListenerKey(requestId, messageType))
            }
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

data class RequestListenerKey(val requestId: Long, val messageType: KClass<out Message>)