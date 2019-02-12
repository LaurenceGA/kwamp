package com.laurencegarmstrong.kwamp.client.core.conversations.scripts;

import com.laurencegarmstrong.kwamp.client.core.conversations.infrastructure.ClientConversation
import com.laurencegarmstrong.kwamp.core.Uri
import com.laurencegarmstrong.kwamp.core.messages.Publish
import io.kotlintest.be
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.containExactly
import io.kotlintest.matchers.haveKey
import io.kotlintest.should
import io.kotlintest.shouldNot
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class LoadTest : StringSpec({
    "Test client sending a high volume of messages" {
        ClientConversation {
            val client = newConnectedTestClient()

            val testTopic = Uri("test.topic")
            val publishArguments = listOf(1, 2, "three")
            val publishArgumentsKw = mapOf("one" to 1, "two" to "two")
            val messagesToPublish = 10000

            repeat(messagesToPublish) {
                launchWithTimeout {
                    client.publish(testTopic, publishArguments, publishArgumentsKw)
                }
            }

            runBlockingWithTimeout {
                val receiveJobs = LinkedList<Job>()
                repeat(messagesToPublish) {
                    logger.debug("Publishing in thread ${Thread.currentThread().name}")
                    receiveJobs.add(launch {
                        client shouldHaveSentMessage { message: Publish ->
                            message.topic should be(testTopic)
                            message.arguments shouldContainExactly publishArguments
                            message.argumentsKw!! should containExactly<String, Any?>(publishArgumentsKw)
                            message.options shouldNot haveKey("acknowledge")
                        }
                    })

                    for (job in receiveJobs) {
                        job.join()
                    }
                }
            }
        }
    }
})