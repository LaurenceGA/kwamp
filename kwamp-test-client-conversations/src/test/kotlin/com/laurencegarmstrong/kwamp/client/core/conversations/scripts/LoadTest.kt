package com.laurencegarmstrong.kwamp.client.core.conversations.scripts;

import com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure.ClientConversation
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Publish
import com.laurencegarmstrong.kwamp.core.messages.Published
import io.kotlintest.be
import io.kotlintest.matchers.containExactly
import io.kotlintest.matchers.haveKey
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class LoadTest : StringSpec({
    "Test client sending a high volume of publish messages" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testTopic = Uri("test.topic")
            val publishArgumentsKw = mapOf("one" to 1, "two" to "two")
            val messagesToPublish = 2000

            val deferredPublications = Array<CompletableDeferred<Long>>(messagesToPublish) {
                CompletableDeferred()
            }
            repeat(messagesToPublish) { eventId ->
                launchWithTimeout {
                    client.publish(testTopic, listOf(eventId), publishArgumentsKw) { publishId ->
                        logger.debug("event $eventId publish acknowledged with pid=$publishId")
                        deferredPublications[eventId].complete(publishId)
                    }
                }
            }

            runBlockingWithTimeout {
                val receiveJobs = LinkedList<Job>()
                repeat(messagesToPublish) { publicationId ->
                    receiveJobs.add(launch {
                        var requestId: Long? = null
                        var eventIndex: Int? = null
                        client shouldHaveSentMessage { message: Publish ->
                            message.topic should be(testTopic)
                            eventIndex = message.arguments!![0] as Int?
                            message.argumentsKw!! should containExactly<String, Any?>(publishArgumentsKw)
                            message.options should haveKey("acknowledge")
                            requestId = message.requestId
                            logger.debug("Publication $publicationId is: Publish(rid=$requestId, eid=$eventIndex)")
                        }

                        logger.debug("Sending client acknowledgment of publish $publicationId -> Published(rid=$requestId, pid=${publicationId.toLong()})")
                        client willBeSentRouterMessage { Published(requestId!!, publicationId.toLong()) }

                        logger.debug("waiting for client to confirm event ${eventIndex!!} acknowledged with pid=${publicationId.toLong()})")
                        deferredPublications[eventIndex!!].await() should be(publicationId.toLong())
                    })

                    receiveJobs.forEach {
                        it.join()
                    }
                }
            }
        }
    }
})