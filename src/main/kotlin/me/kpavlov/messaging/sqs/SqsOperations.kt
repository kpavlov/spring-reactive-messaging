package me.kpavlov.messaging.sqs

import org.springframework.integration.acks.AcknowledgmentCallback
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.model.Message
import java.util.concurrent.CompletionStage

internal sealed interface SqsOperations {
    fun getQueueUrl(): CompletionStage<String>
}

internal interface SqsConsumerOperations : SqsOperations {

    fun receiveMessages(queueUrl: String): CompletionStage<List<Message>>

    fun <T> acknowledge(
        queueUrl: String,
        message: org.springframework.messaging.Message<T>,
        status: AcknowledgmentCallback.Status
    ): Mono<Boolean>
}

internal interface SqsPublisherOperations : SqsOperations {

    fun send(message: Message): CompletionStage<String>
}
