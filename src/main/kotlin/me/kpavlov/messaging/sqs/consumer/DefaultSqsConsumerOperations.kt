package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.sqs.DefaultSqsOperations
import me.kpavlov.messaging.sqs.SqsConsumerOperations
import org.slf4j.Logger
import org.springframework.integration.acks.AcknowledgmentCallback
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

internal class DefaultSqsConsumerOperations(
    private val sqsClient: SqsAsyncClient,
    private val properties: SqsConsumerProperties,
    private val logger: Logger
) : DefaultSqsOperations(sqsClient, properties), SqsConsumerOperations {

    override fun receiveMessages(queueUrl: String): CompletionStage<List<Message>> {
        val receiveMessageRequest = ReceiveMessageRequest
            .builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(properties.batchSize)
            .attributeNames(QueueAttributeName.ALL)
            .waitTimeSeconds(properties.waitTimeSeconds)
            .build()
        return sqsClient.receiveMessage(receiveMessageRequest)
            .thenApply { response: ReceiveMessageResponse ->
                val messages = response.messages()
                if (response.hasMessages()) {
                    logger.debug("Received SQS Messages on {}: {}", properties.queueName, messages)
                } else {
                    logger.trace("Received NO SQS Messages on {}", properties.queueName)
                }
                messages
            }
            .exceptionallyCompose { throwable ->
                logger.error("Can't receive messages on {}", properties.queueName, throwable)
                // sink.error(throwable)
                CompletableFuture.failedStage(throwable)
            }
    }

    override fun <T> acknowledge(
        queueUrl: String,
        message: org.springframework.messaging.Message<T>,
        status: AcknowledgmentCallback.Status
    ): Mono<Boolean> = when (status) {
        AcknowledgmentCallback.Status.ACCEPT -> {
            logger.info("Message was successfully processed: {} - {}", message, status)
            deleteMessage(queueUrl, message)
        }

        AcknowledgmentCallback.Status.REJECT -> {
            logger.info(
                "Message processing was not successful, but it is not retryable: " + "{} - {}",
                message,
                status
            )
            deleteMessage(queueUrl, message)
        }

        else -> {
            logger.warn("Message processing was not successful, will retry: {} - {}", message, status)
            Mono.empty()
        }
    }

    private fun <T> deleteMessage(
        queueUrl: String,
        message: org.springframework.messaging.Message<T>
    ): Mono<Boolean> {
        val receiptHandle = SqsMessageHeaders.getReceiptHandle(message)
        logger.info("Deleting message with receipt handle: {}", receiptHandle)
        val deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(queueUrl)
            .receiptHandle(receiptHandle).build()
        return Mono.fromCompletionStage(
            sqsClient.deleteMessage(deleteMessageRequest)
                .thenAccept {
                    logger.info(
                        "Message with receipt handle={} was deleted.",
                        receiptHandle
                    )
                }
                .thenApply { true }
                .exceptionally { throwable ->
                    logger.error("Message with receipt handle={} was NOT deleted.", receiptHandle, throwable)
                    false
                }
        )
    }
}
