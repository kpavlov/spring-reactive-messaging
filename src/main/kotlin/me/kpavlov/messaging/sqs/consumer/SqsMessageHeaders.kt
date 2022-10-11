package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.ReactiveAcknowledgmentCallback
import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import java.util.*

internal class SqsMessageHeaders(headers: Map<String, Any>, id: UUID, timestamp: Long) :
    MessageHeaders(headers, id, timestamp) {
    companion object {
        /**
         * The key for the message _Receipt Handle_ header.
         */
        const val RECEIPT_HANDLE = "receiptHandle"

        fun getAcknowledgmentCallback(message: Message<*>): ReactiveAcknowledgmentCallback? {
            return message.headers.get(
                IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
                ReactiveAcknowledgmentCallback::class.java
            )
        }

        /**
         * Returns SQS Message _Receipt handle_
         *
         * > Every time you receive a message from a queue, you receive a receipt handle
         * > for that message. This handle is associated with the action of receiving the message,
         * > not with the message itself. To delete the message or to change the message visibility,
         * > you must provide the receipt handle (not the message ID).
         *
         * @see <a href="https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-queue-message-identifiers.html#receipt-handle">Receipt handle</a>
         */
        fun getReceiptHandle(message: Message<*>): String? {
            return message.headers.get(
                RECEIPT_HANDLE,
                String::class.java
            )
        }
    }
}
