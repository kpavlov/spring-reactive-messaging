package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.sqs.SqsConsumerOperations
import org.slf4j.LoggerFactory
import org.springframework.integration.IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.function.Function

private val logger = LoggerFactory.getLogger(SqsSourceFactory::class.java)

internal class DefaultSqsSourceFactory(
    private val template: SqsConsumerOperations
) : SqsSourceFactory {

    override fun <T> createFlux(
        queueUrl: String,
        properties: SqsConsumerProperties,
        converter: SqsMessageConverter<T>
    ): Flux<Message<T>> {
        val queueName = properties.queueName
        logger.info("Subscribing to SQS events on {}", queueName)
        var flux = Flux.generate { sink ->
            template.receiveMessages(queueUrl)
                .thenAccept { sink.next(it) }
                .exceptionally { throwable -> sink.error(throwable); null }
                .toCompletableFuture().join() // SynchronousSink requires SYNCHRONOUS operation here
        }
        if (properties.poolingDelayMillis > 0) {
            flux = flux
                .sample(Duration.ofMillis(properties.poolingDelayMillis.toLong()))
        }

        return flux
            .concatMapIterable(Function.identity())
            .map { converter.toMessage(it) }
            .map {
                MessageBuilder
                    .fromMessage(it)
                    .setHeader(ACKNOWLEDGMENT_CALLBACK, SqsAcknowledgment(template, queueUrl, it))
                    .build()
            }
    }
}

public interface SqsSourceFactory {
    public fun <T> createFlux(
        queueUrl: String,
        properties: SqsConsumerProperties,
        converter: SqsMessageConverter<T>
    ): Flux<Message<T>>
}
