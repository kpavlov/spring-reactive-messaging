package me.kpavlov.messaging

import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.ReadinessState
import org.springframework.context.event.EventListener

public abstract class MessageConsumerTemplate {

    public abstract fun start()

    public abstract fun stop()

    @EventListener(AvailabilityChangeEvent::class)
    public fun onAvailabilityChanged(event: AvailabilityChangeEvent<ReadinessState>) {
        if (event.state == ReadinessState.ACCEPTING_TRAFFIC) {
            start()
        } else {
            stop()
        }
    }
}
