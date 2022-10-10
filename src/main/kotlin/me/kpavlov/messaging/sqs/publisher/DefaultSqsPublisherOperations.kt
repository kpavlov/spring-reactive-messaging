package me.kpavlov.messaging.sqs.publisher

import me.kpavlov.messaging.sqs.DefaultSqsOperations
import me.kpavlov.messaging.sqs.SqsProperties
import me.kpavlov.messaging.sqs.SqsPublisherOperations
import org.slf4j.Logger
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

internal class DefaultSqsPublisherOperations(
    private val sqsClient: SqsAsyncClient,
    properties: SqsProperties,
    private val logger: Logger
) : DefaultSqsOperations(sqsClient, properties), SqsPublisherOperations {

    override fun send(message: Message): CompletionStage<String> {
        return getQueueUrl().thenCompose {
            doSend(message, it)
        }
    }

    private fun doSend(message: Message, queueUrl: String): CompletionStage<String> {
        logger.trace("Sending message: {}", message)
        val sendMessageRequest = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(message.body())
            .build()
        return sqsClient.sendMessage(sendMessageRequest)
            .thenApply {
                logger.info(
                    "Message sent. MessageId={}",
                    it.messageId()
                )
                it.messageId()
            }
            .exceptionallyCompose { throwable ->
                logger.error("Can't send message", throwable)
                CompletableFuture.failedStage(throwable)
            }
    }
}
