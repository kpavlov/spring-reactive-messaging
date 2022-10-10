package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.ReactiveAcknowledgmentCallback
import me.kpavlov.messaging.sqs.SqsConsumerOperations
import org.springframework.integration.acks.AcknowledgmentCallback
import reactor.core.publisher.Mono

internal class SqsAcknowledgment<T>(
    private val template: SqsConsumerOperations,
    private val queueUrl: String,
    private val message: org.springframework.messaging.Message<T>
) : AcknowledgmentCallback, ReactiveAcknowledgmentCallback {

    override fun acknowledgeAsync(status: AcknowledgmentCallback.Status): Mono<Boolean> =
        template.acknowledge(queueUrl, message, status)

    override fun acknowledge(status: AcknowledgmentCallback.Status) {
        template.acknowledge(queueUrl, message, status).block()
    }
}
