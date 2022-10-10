package me.kpavlov.messaging.sqs

import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse
import java.util.concurrent.CompletionStage

internal open class DefaultSqsOperations(
    private val sqsClient: SqsAsyncClient,
    private val properties: SqsProperties
) : SqsOperations {

    override fun getQueueUrl(): CompletionStage<String> {
        return sqsClient.getQueueUrl(
            GetQueueUrlRequest.builder().queueName(properties.getQueueName()).build()
        )
            .thenApply { obj: GetQueueUrlResponse -> obj.queueUrl() }
    }
}
