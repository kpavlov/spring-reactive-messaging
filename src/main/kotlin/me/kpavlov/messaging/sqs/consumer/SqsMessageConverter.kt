package me.kpavlov.messaging.sqs.consumer

import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.support.MessageBuilder
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

@FunctionalInterface
public class SqsMessageConverter<T>(
    private val messageBodyDecoder: Function<String, T>
) {
    public fun toMessage(sqsMessage: Message): org.springframework.messaging.Message<T> {
        val payload = messageBodyDecoder.apply(sqsMessage.body())
        val attributes = sqsMessage.attributes()
        val headersMap = HashMap<String, Any>()
        headersMap[MessageHeaders.ID] = UUID.fromString(sqsMessage.messageId())
        headersMap[SqsMessageHeaders.RECEIPT_HANDLE] = sqsMessage.receiptHandle()
        attributes[MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT]?.let {
            headersMap[IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT] = AtomicInteger(it.toInt())
        }
        val headers = SqsMessageHeaders(
            headersMap,
            UUID.fromString(sqsMessage.messageId()),
            attributes[MessageSystemAttributeName.SENT_TIMESTAMP]!!.toLong()
        )
        return MessageBuilder.createMessage(payload, headers)
    }
}
