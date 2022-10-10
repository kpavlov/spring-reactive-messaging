package me.kpavlov.messaging

import org.springframework.integration.acks.AcknowledgmentCallback
import org.springframework.messaging.Message
import reactor.core.publisher.Mono

@FunctionalInterface
public fun interface ReactiveMessageHandler<T> {
    public fun process(cmd: Message<T>): Mono<AcknowledgmentCallback.Status>
}
