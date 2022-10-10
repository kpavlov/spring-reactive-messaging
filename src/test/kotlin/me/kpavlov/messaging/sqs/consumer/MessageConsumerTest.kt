package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.ReactiveMessageHandler
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.integration.acks.AcknowledgmentCallback
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.*
import java.time.Clock
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Function

private val logger = LoggerFactory.getLogger(MessageConsumerTest::class.java)

@ExtendWith(MockitoExtension::class)
internal class MessageConsumerTest {

    @Mock
    lateinit var sqsClient: SqsAsyncClient

    @Mock
    lateinit var messageProcessor: ReactiveMessageHandler<String>

    private val messages: Queue<String> = ConcurrentLinkedQueue()

    @Volatile
    private var latch: CountDownLatch? = null

    private val queueName = "test-queue"

    private val queueUrl = "https://sqs.us-east-2.amazonaws.com/123456789012/$queueName"

    lateinit var props: SqsConsumerProperties

    lateinit var subject: SqsMessageConsumer<String>

    @BeforeEach
    fun beforeEach() {
        props = SqsConsumerProperties(
            queueName,
            1,
            1,
            1,
            1,
            poolingDelayMillis = 0 // disable sampling for this unit test
        )
        subject = SqsMessageConsumer(
            sqsClient = sqsClient,
            properties = props,
            messageProcessor = messageProcessor,
            messageBodyDecoder = Function.identity()
        )
    }

    @AfterEach
    fun afterEach() {
        subject.stop()
        messages.clear()
    }

    @Test
    fun `Should process 3 messages`() {
        val payloads = arrayOf("Hello, World", "Hooray", "Bye")

        messages.addAll(payloads)

        `should get queueUrl`()

        `should receive messages`()

        whenever(messageProcessor.process(any()))
            .then { answer ->
                val message = answer.getArgument<Any>(0)
                logger.info("Processing: {}", message)
                @Suppress("ReactiveStreamsUnusedPublisher")
                (Mono.just(AcknowledgmentCallback.Status.ACCEPT))
            }

        `should delete messages`()

        // when
        subject.start()

        // then
        val completed = latch?.await(10, TimeUnit.SECONDS)
        Assertions.assertThat(completed).`as`("All consumed").isTrue
    }

    private fun `should receive messages`() {
        latch = CountDownLatch(messages.size)
        whenever(
            sqsClient.receiveMessage(
                ArgumentMatchers.any(
                    ReceiveMessageRequest::class.java
                )
            )
        )
            .thenAnswer {
                val nextMessage = messages.poll()
                val response = when {
                    nextMessage != null -> {
                        val messageId = UUID.randomUUID().toString()
                        val message = Message.builder()
                            .body(nextMessage)
                            .messageId(messageId)
                            .receiptHandle(messageId + "#" + UUID.randomUUID())
                            .attributes(
                                mapOf(
                                    MessageSystemAttributeName.SENT_TIMESTAMP to Clock.systemUTC()
                                        .millis().toString()
                                )
                            )
                            .build()

                        ReceiveMessageResponse.builder()
                            .messages(message)
                            .build()
                    }

                    else -> {
                        ReceiveMessageResponse.builder()
                            .build()
                    }
                }
                CompletableFuture.completedFuture(response)
            }
    }

    private fun `should delete messages`() {
        whenever(
            sqsClient.deleteMessage(
                ArgumentMatchers.any(
                    DeleteMessageRequest::class.java
                )
            )
        )
            .thenAnswer {
                val response = DeleteMessageResponse.builder()
                    .build()
                latch?.countDown()
                CompletableFuture.completedFuture(response)
            }
    }

    private fun `should get queueUrl`() {
        whenever(
            sqsClient.getQueueUrl(
                ArgumentMatchers.argThat<GetQueueUrlRequest> {
                    it.queueName() == queueName
                }
            )
        ).thenReturn(
            CompletableFuture.completedFuture(
                GetQueueUrlResponse.builder()
                    .queueUrl(queueUrl)
                    .build()
            )
        )
    }

    private fun `should receive messages`(payloads: Array<String>) {
        val messages = mutableListOf<Message>()
        payloads.forEach {
            messages += Message.builder()
                .body(it)
                .receiptHandle(it.hashCode().toString(16))
                .build()
        }

        whenever(
            sqsClient.receiveMessage(
                ArgumentMatchers.argThat<ReceiveMessageRequest> {
                    it.queueUrl() == queueUrl &&
                        it.maxNumberOfMessages() == props.batchSize &&
                        it.waitTimeSeconds() == props.waitTimeSeconds
                }
            )
        )
            .thenReturn(
                CompletableFuture.completedFuture(
                    ReceiveMessageResponse.builder()
                        .messages(messages)
                        .build()
                )
            )
    }
}
