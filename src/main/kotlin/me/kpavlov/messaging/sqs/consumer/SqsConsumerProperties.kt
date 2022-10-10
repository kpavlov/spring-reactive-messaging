package me.kpavlov.messaging.sqs.consumer

import me.kpavlov.messaging.sqs.SqsProperties

@JvmRecord
public data class SqsConsumerProperties(
    val queueName: String,
    val executorThreads: Int = Runtime.getRuntime().availableProcessors(),
    val backpressureBufferSize: Int = 32,
    /**
     *  Number of messages per batch
     */
    val batchSize: Int = 10,

    /**
     * SQS [ReceiveMessageWaitTimeSeconds](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-short-and-long-polling.html#sqs-long-polling)
     * parameter.
     */
    val waitTimeSeconds: Int = 1,

    /**
     * Delay between pooling messages from SQS
     */
    val poolingDelayMillis: Int = 100
) : SqsProperties {
    override fun getQueueName(): String = queueName
}
