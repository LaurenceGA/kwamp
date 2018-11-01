package com.laurencegarmstrong.kwamp.client.core

import com.laurencegarmstrong.kwamp.core.messages.Error
import com.laurencegarmstrong.kwamp.core.messages.Message
import com.laurencegarmstrong.kwamp.core.messages.RequestMessage
import com.laurencegarmstrong.kwamp.core.toWampErrorException
import kotlinx.coroutines.*
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

    inline fun <reified T : Message> registerListenerWithErrorHandler(requestId: Long): Deferred<T> =
        registerListenerWithErrorHandler(requestId, T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Message> registerTypeListener(messageType: KClass<out T>): Deferred<T> =
        GlobalScope.async {
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

    fun <T : Message> registerListenerWithErrorHandler(requestId: Long, messageType: KClass<out T>) =
        CompletableDeferred<T>().also {
            applyListenersToDeferred(it, requestId, messageType)
        }

    private fun <T : Message> applyListenersToDeferred(
        completableDeferredMessage: CompletableDeferred<T>,
        requestId: Long,
        messageType: KClass<out T>
    ) = GlobalScope.launch {
        val deferredMessage = registerListener(requestId, messageType)
        val deferredErrorMessage = registerListener<Error>(requestId)
        launch {
            val message = deferredMessage.await()
            deferredErrorMessage.cancel()
            completableDeferredMessage.complete(message)
        }
        launch {
            val errorMessage = deferredErrorMessage.await()
            deferredMessage.cancel()
            completableDeferredMessage.completeExceptionally(errorMessage.toWampErrorException())
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