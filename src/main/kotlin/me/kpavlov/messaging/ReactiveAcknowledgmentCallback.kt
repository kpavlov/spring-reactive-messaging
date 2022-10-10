package me.kpavlov.messaging

import org.springframework.integration.acks.AcknowledgmentCallback
import reactor.core.publisher.Mono

public interface ReactiveAcknowledgmentCallback : AcknowledgmentCallback {

    override fun acknowledge(status: AcknowledgmentCallback.Status) {
        acknowledgeAsync().block()
    }

    public fun acknowledgeAsync(status: AcknowledgmentCallback.Status): Mono<Boolean>

    public fun acknowledgeAsync(): Mono<Boolean> = acknowledgeAsync(AcknowledgmentCallback.Status.ACCEPT)
}
