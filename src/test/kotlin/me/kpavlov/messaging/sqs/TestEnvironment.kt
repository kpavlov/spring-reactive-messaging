package me.kpavlov.messaging.sqs

import com.github.kpavlov.maya.sqs.LocalSqs

public object TestEnvironment {
    @JvmField
    public val SQS: LocalSqs = LocalSqs("sqs-queues.conf")

    init {
        SQS.start()
    }
}
