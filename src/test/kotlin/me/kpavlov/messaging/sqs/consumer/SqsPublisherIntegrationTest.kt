package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.sqs.TestEnvironment
import me.kpavlov.messaging.sqs.publisher.DefaultSqsPublisherOperations
import me.kpavlov.messaging.sqs.publisher.SqsPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.integration.acks.AcknowledgmentCallback
import org.springframework.integration.channel.FluxMessageChannel
import org.springframework.integration.support.MessageBuilder
import reactor.core.publisher.Mono
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Function

internal class SqsPublisherIntegrationTest {

    private val logger = LoggerFactory.getLogger(SqsPublisherIntegrationTest::class.java)

    private val sqsClient = SqsAsyncClient.builder()
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("key", "secret")
            )
        )
        .endpointOverride(URI.create(TestEnvironment.SQS.endpointUrl()))
        .region(Region.US_EAST_1)
        .build()

    private val properties = SqsConsumerProperties(
        queueName = "SqsPublisherIntegrationTest"
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

    private lateinit var messageChannel: FluxMessageChannel

    @BeforeEach
    fun `before each`() {
        messageChannel = FluxMessageChannel()
        val template = DefaultSqsPublisherOperations(sqsClient, properties, logger)
        messageChannel.subscribe(SqsPublisher(template))
    }

    @Test
    fun `should send and receive message`() {
        val payload = "Hello, World!"
        val message = MessageBuilder.withPayload(payload).build()

        messageChannel.send(message, 5_000)

        latch = CountDownLatch(1)

        // when
        consumer.start()

        // then
        latch.await()

        assertThat(consumedMessages).containsExactly(payload)
    }
}
