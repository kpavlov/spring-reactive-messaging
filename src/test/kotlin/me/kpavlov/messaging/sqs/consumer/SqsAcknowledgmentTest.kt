package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.sqs.SqsConsumerOperations
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.integration.acks.AcknowledgmentCallback.Status
import org.springframework.messaging.Message
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
internal class SqsAcknowledgmentTest {

    private lateinit var subject: SqsAcknowledgment<*>

    @Mock
    lateinit var message: Message<*>

    @Mock
    lateinit var template: SqsConsumerOperations

    lateinit var queueUrl: String

    @BeforeEach
    fun setUp() {
        queueUrl = "https://sqs.us-east-2.amazonaws.com/${randomNumeric(12)}/queueName"
        subject = SqsAcknowledgment(template, queueUrl, message)
    }

    @ParameterizedTest
    @EnumSource(Status::class)
    fun `should acknowledge`(status: Status) {
        // given
        whenever(template.acknowledge(queueUrl, message, status)).thenReturn(Mono.just(true))

        // when
        subject.acknowledge(status)

        // then
        verify(template).acknowledge(queueUrl, message, status)
    }
}
