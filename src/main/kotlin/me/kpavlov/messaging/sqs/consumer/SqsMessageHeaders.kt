package me.kpavlov.messaging.sqs.consumer

import org.springframework.messaging.MessageHeaders
import java.util.*

internal class SqsMessageHeaders(headers: Map<String, Any>, id: UUID, timestamp: Long) :
    MessageHeaders(headers, id, timestamp) {
    companion object {
        /**
         * The key for the message ReceiptHandle.
         */
        const val RECEIPT_HANDLE = "receiptHandle"
    }
}
