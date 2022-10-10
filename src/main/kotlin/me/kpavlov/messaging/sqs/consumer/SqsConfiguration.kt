package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.ReactiveMessageHandler
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.acks.AcknowledgmentCallback
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.util.function.Function

@Configuration
@ConfigurationProperties(prefix = "sqs")
internal open class SqsConfiguration(
    private val properties: SqsConsumerProperties,
    private val sqsClient: SqsAsyncClient
) {

    private fun messageProcessor(): ReactiveMessageHandler<String> {
        return ReactiveMessageHandler { Mono.just(AcknowledgmentCallback.Status.ACCEPT) }
    }

    @Bean
    open fun createConsumer(): SqsMessageConsumer<String> {
        return SqsMessageConsumer(
            sqsClient = sqsClient,
            properties = properties,
            messageProcessor = messageProcessor(),
            messageBodyDecoder = Function.identity()
        )
    }
}
