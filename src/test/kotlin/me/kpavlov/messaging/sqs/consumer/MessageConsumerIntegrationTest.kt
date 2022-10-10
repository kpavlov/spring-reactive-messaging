package me.kpavlov.messaging.sqs.consumer

import com.github.kpavlov.maya.sqs.SqsMessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.kpavlov.messaging.sqs.TestEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.integration.acks.AcknowledgmentCallback
import reactor.core.publisher.Mono
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Function

private const val QUEUE_NAME = "MessageConsumerIntegrationTest"

internal class MessageConsumerIntegrationTest {

    private val sqsClient = SqsAsyncClient.builder()
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("key", "secret")
            )
        )
        .endpointOverride(URI.create(sqs.endpointUrl()))
        .region(Region.US_EAST_1)
        .build()

    private val sender = SqsMessageSender(
        sqsClient,
        QUEUE_NAME,
        Function.identity()
    )

    private val properties = SqsConsumerProperties(
        queueName = QUEUE_NAME
    )

    private val consumedMessages = LinkedBlockingQueue<String>()

    @Volatile
    private lateinit var latch: CountDownLatch
    private val consumer = SqsMessageConsumer(
        sqsClient = sqsClient,
        properties = properties,
        messageProcessor = {
            consumedMessages.add(it.payload)
            latch.countDown()
            Mono.just(AcknowledgmentCallback.Status.ACCEPT)
        },
        messageBodyDecoder = Function.identity()
    )

    @Test
    fun `should send message`(): Unit = runBlocking(Dispatchers.IO) {
        val message = "Hello"
        val messageId = sender.sendMessageAsync(message).await()
        assertThat(messageId).isNotBlank
        latch = CountDownLatch(1)

        // when
        consumer.start()

        // then
        latch.await()

        assertThat(consumedMessages).containsExactly(message)
    }

    companion object {
        private val sqs = TestEnvironment.SQS
    }
}
