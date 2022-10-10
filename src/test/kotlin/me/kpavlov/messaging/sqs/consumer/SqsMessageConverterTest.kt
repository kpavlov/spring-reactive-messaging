package me.kpavlov.messaging.sqs.consumer

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.messaging.MessageHeaders
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import java.util.*
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

internal class SqsMessageConverterTest {
    private val subject = SqsMessageConverter(
        Function.identity()
    )
    private lateinit var payload: String
    private var deliveryAttempt = 0
    private var timestamp: Long = 0

    @BeforeEach
    fun setUp() {
        payload = RandomStringUtils.randomAlphanumeric(5)
        deliveryAttempt = 396
        timestamp = 1664812443063L
    }

    /*
    Received: [Message(MessageId=144a5cbb-6889-442f-b0e0-95117f14b159, ReceiptHandle=144a5cbb-6889-442f-b0e0-95117f14b159#4fd6261e-137c-4d1c-a9e6-9699e119175f, MD5OfBody=f6e8323b848887077a7e836f92ecbf8f, Body={"requestSid":"RQ340d3d01b75fcab47431279db376c12b","accountSid":"ACfc13a9d80721ab66d1612a85dd28d804","organizationSid":"ORfe0215e693a927840023d305cdec7426"}, Attributes={ApproximateReceiveCount=396, SentTimestamp=1664812140102, SequenceNumber=19, MessageGroupId=ACfc13a9d80721ab66d1612a85dd28d804, SenderId=127.0.0.1, MessageDeduplicationId=ACfc13a9d80721ab66d1612a85dd28d804, ApproximateFirstReceiveTimestamp=1664812140103}), Message(MessageId=33dca8ce-2ad2-44b0-a787-0de5eb37af6c, ReceiptHandle=33dca8ce-2ad2-44b0-a787-0de5eb37af6c#f619dfd6-5c3c-4df1-aaa1-e2f2335a3d42, MD5OfBody=994fe5b01ac1cbafa3821b737db66a8b, Body={"requestSid":"RQ53496068204e086e716a38161e720269","accountSid":"AC4ff5a51b57bf3a255b788a23d5a20994","organizationSid":"OR5fa39314f5d87d07ae2fba66dcbddc05"}, Attributes={ApproximateReceiveCount=297, SentTimestamp=1664812241089, SequenceNumber=24, MessageGroupId=AC4ff5a51b57bf3a255b788a23d5a20994, SenderId=127.0.0.1, MessageDeduplicationId=AC4ff5a51b57bf3a255b788a23d5a20994, ApproximateFirstReceiveTimestamp=1664812241089}), Message(MessageId=8caf2854-ad42-4f1a-b889-2df9dc7b1f2a, ReceiptHandle=8caf2854-ad42-4f1a-b889-2df9dc7b1f2a#79e98c99-3b57-4d76-a786-e8f0d30c35b6, MD5OfBody=d1fd6a2e89a56bf0ed99b6633293853b, Body={"requestSid":"RQ1b91140f71c7d69dcb2355d4cdd58c6d","accountSid":"ACf1fff0f2f882fa7dbaa7c0df97e00110","organizationSid":"OR7b3d007393d953eac70e8a7fed7e5215"}, Attributes={ApproximateReceiveCount=99, SentTimestamp=1664812443063, SequenceNumber=34, MessageGroupId=ACf1fff0f2f882fa7dbaa7c0df97e00110, SenderId=127.0.0.1, MessageDeduplicationId=ACf1fff0f2f882fa7dbaa7c0df97e00110, ApproximateFirstReceiveTimestamp=1664812443063}), Message(MessageId=59177334-3092-4ccf-bf2b-f65bba66e1ee, ReceiptHandle=59177334-3092-4ccf-bf2b-f65bba66e1ee#2a366d13-7f4c-4b54-99e7-50f55d2378fb, MD5OfBody=d7403dcdeee6cfb4974b4e581fa54e37, Body={"requestSid":"RQbf5042ce122ad40b4d0973c5ae70b214","accountSid":"AC07478a087fdb9ad83eb58abcd5590982","organizationSid":"ORbdff6f069f5e23fbbc21d350ec1b3caf"}, Attributes={ApproximateReceiveCount=198, SentTimestamp=1664812342122, SequenceNumber=29, MessageGroupId=AC07478a087fdb9ad83eb58abcd5590982, SenderId=127.0.0.1, MessageDeduplicationId=AC07478a087fdb9ad83eb58abcd5590982, ApproximateFirstReceiveTimestamp=1664812342122})]
     */
    @Test
    fun shouldConvertMessageWithAttributes() {
        val sqsMessage = Message.builder()
            .body(payload)
            .messageId(MESSAGE_ID)
            .receiptHandle(RECEIPT_HANDLE)
            .attributes(
                Map.of(
                    MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                    deliveryAttempt.toString(),
                    MessageSystemAttributeName.MESSAGE_DEDUPLICATION_ID,
                    "7b3d007393d953eac70e8a7fed7e5215",
                    MessageSystemAttributeName.MESSAGE_GROUP_ID,
                    "f1fff0f2f882fa7dbaa7c0df97e00110",
                    MessageSystemAttributeName.SENT_TIMESTAMP,
                    timestamp.toString(),
                    MessageSystemAttributeName.SEQUENCE_NUMBER,
                    "34"
                )
            )
            .build()
        val result = subject.toMessage(sqsMessage)
        Assertions.assertThat(result.payload).isEqualTo(payload)
        val headers = result.headers
        Assertions.assertThat(headers.id).isEqualTo(UUID.fromString(MESSAGE_ID))
        Assertions.assertThat(headers[MessageHeaders.ID] as UUID?).isEqualTo(UUID.fromString(MESSAGE_ID))
        Assertions.assertThat(headers.timestamp).isEqualTo(timestamp)
        Assertions.assertThat(headers[MessageHeaders.TIMESTAMP] as Long?).isEqualTo(timestamp)
        Assertions.assertThat(headers[IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT] as AtomicInteger?)
            .hasValue(deliveryAttempt)
        Assertions.assertThat(headers["receiptHandle"] as String?).isEqualTo(RECEIPT_HANDLE)
    }

    @Test
    fun shouldConvertMessageWithoutAttributes() {
        val payload = RandomStringUtils.randomAlphanumeric(5)
        val messageId = "144a5cbb-6889-442f-b0e0-95117f14b159"
        val receiptHandle = "144a5cbb-6889-442f-b0e0-95117f14b159#4fd6261e-137c-4d1c-a9e6-9699e119175f"
        val timestamp = 1664812443063L
        val sqsMessage = Message.builder()
            .body(payload)
            .messageId(messageId)
            .receiptHandle(receiptHandle)
            .attributes(Map.of(MessageSystemAttributeName.SENT_TIMESTAMP, timestamp.toString()))
            .build()
        val result = subject.toMessage(sqsMessage)
        Assertions.assertThat(result.payload).isEqualTo(payload)
        val headers = result.headers
        Assertions.assertThat(headers.id).isEqualTo(UUID.fromString(messageId))
        Assertions.assertThat(headers[MessageHeaders.ID] as UUID?).isEqualTo(UUID.fromString(messageId))
        Assertions.assertThat(headers.timestamp).isEqualTo(timestamp)
        Assertions.assertThat(headers[MessageHeaders.TIMESTAMP] as Long?).isEqualTo(timestamp)
        Assertions.assertThat(headers["receiptHandle"] as String?).isEqualTo(receiptHandle)
    }

    companion object {
        private const val MESSAGE_ID = "144a5cbb-6889-442f-b0e0-95117f14b159"
        private const val RECEIPT_HANDLE = "144a5cbb-6889-442f-b0e0-95117f14b159#4fd6261e-137c-4d1c-a9e6-9699e119175f"
    }
}
