package me.kpavlov.messaging.sqs.consumer

import io.vavr.Tuple
import io.vavr.Tuple2
import me.kpavlov.messaging.MessageConsumerTemplate
import me.kpavlov.messaging.ReactiveAcknowledgmentCallback
import me.kpavlov.messaging.ReactiveMessageHandler
import me.kpavlov.messaging.sqs.SqsConsumerOperations
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.integration.acks.AcknowledgmentCallback
import org.springframework.messaging.Message
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

/**
 * @see [
 * Processing SQS Messages using Spring Boot and Project Reactor
](http://www.java-allandsundry.com/2020/03/processing-sqs-messages-using-spring.html) *
 */
public class SqsMessageConsumer<T> public constructor(
    sqsClient: SqsAsyncClient,
    private val properties: SqsConsumerProperties,
    messageProcessor: ReactiveMessageHandler<T>,
    messageBodyDecoder: Function<String, T>
) : MessageConsumerTemplate() {
    private val logger: Logger
    private val queueName: String = properties.queueName
    private val sqsClient: SqsAsyncClient
    private val messageProcessor: ReactiveMessageHandler<T>
    private val started = AtomicBoolean(false)
    private val converter = SqsMessageConverter(messageBodyDecoder)
    private val template: SqsConsumerOperations
    private val sourceFactory: SqsSourceFactory

    private val subscriptionRef = AtomicReference<Disposable?>()

    init {
        logger = LoggerFactory.getLogger(SqsMessageConsumer::class.java.toString() + "[" + queueName + "]")
        this.sqsClient = sqsClient
        this.messageProcessor = messageProcessor
        this.template = DefaultSqsConsumerOperations(sqsClient, properties, logger)
        this.sourceFactory = DefaultSqsSourceFactory(template)
    }

    override fun start() {
        if (!started.compareAndSet(false, true)) {
            return
        }
        if (subscriptionRef.get() != null) {
            return
        }
        logger.debug("Starting SQS Consumer")
        template.getQueueUrl()
            .thenAccept { queueUrl: String ->
                logger.info("Subscribing to SQS events on {}", queueName)
                val source = sourceFactory.createFlux(
                    queueUrl,
                    properties,
                    converter = converter
                )
                val subscription = createFlow(source)
                    .publishOn(Schedulers.newSingle("publisher"))
                    .subscribeOn(
                        Schedulers.newParallel(
                            "sqs-worker:$queueName",
                            properties.executorThreads
                        )
                    )
                    .subscribe(
                        { nextValue: Boolean -> logger.info("Next element processed: {}", nextValue) },
                        { error: Throwable ->
                            logger.error(
                                "SQS Subscription completed with error: {}",
                                error.message,
                                error
                            )
                        }
                    ) { logger.info("SQS Subscription completed.") }
                logger.info("Subscribed to SQS events on {}: {}", queueName, subscription)
                if (!subscriptionRef.compareAndSet(null, subscription)) {
                    subscription.dispose()
                }
            }
    }

    private fun createFlow(source: Flux<Message<T>>): Flux<Boolean> {
        return source
            // backpressure buffer
            .onBackpressureBuffer(properties.backpressureBufferSize)
            .log("after backpressure")
            .flatMap({ message ->
                logger.info("Processing message: {}", message)
                messageProcessor.process(message)
                    .map { result: AcknowledgmentCallback.Status -> Tuple.of(message, result) }
            }, properties.executorThreads)
            .log("after processor")
            .flatMap { tuple: Tuple2<Message<T>, AcknowledgmentCallback.Status> ->
                val message = tuple._1()
                val result = tuple._2()
                val callback = message.headers.get(
                    IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
                    ReactiveAcknowledgmentCallback::class.java
                )
                if (callback != null && callback.isAutoAck && !callback.isAcknowledged) {
                    callback.acknowledgeAsync(result)
                } else {
                    Mono.just(false)
                }
            }
            .doOnError { throwable -> logger.error("Error in SQS processing stream: {}", queueName, throwable) }
            .retry()
    }

    override fun stop() {
        if (started.compareAndSet(true, false)) {
            logger.info("Stopping SQS Consumer")
            subscriptionRef.getAndSet(null)?.dispose()
        }
    }
}
