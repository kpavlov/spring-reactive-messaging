package me.kpavlov.messaging.sqs.publisher

import me.kpavlov.messaging.sqs.SqsPublisherOperations
import org.springframework.integration.handler.AbstractMessageHandler
import org.springframework.messaging.Message

internal class SqsPublisher(
    private val template: SqsPublisherOperations
) : AbstractMessageHandler() {

    override fun handleMessageInternal(message: Message<*>) {
        val payload = message.payload.toString()
        val sqsMessage = software.amazon.awssdk.services.sqs.model.Message.builder()
            .body(payload).build()
        template.send(sqsMessage)
            .toCompletableFuture().join()
    }
}
