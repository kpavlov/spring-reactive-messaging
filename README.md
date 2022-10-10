# spring-reactive-messaging

Reactive producer/consumer components
inspired by [Spring Messaging][SpringMessaging]
and [Spring Integration][SpringIntegration],
implemented using [Project Reactor](https://projectreactor.io/).

## Motivation

[Spring Integration][SpringIntegration] provides a framework for connecting to messaging systems,
but there is no support for [AWS SQS][sqs].
Also, [Spring Messaging][SpringMessaging] has synchronous API which is not designed
for Reactive Streams.

So, my immediate goal is to implement reactive message producer and consumer
with a functionality similar to Spring Integration.

## Features

* Common abstractions
    - [ReactiveMessageHandler](src/main/kotlin/me/kpavlov/messaging/ReactiveMessageHandler.kt) - a reactive version
      of [MessageHandler](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/messaging/MessageHandler.html)
      interface.
    - [ReactiveAcknowledgmentCallback](src/main/kotlin/me/kpavlov/messaging/ReactiveAcknowledgmentCallback.kt) - a
      reactive version
      of [AcknowledgmentCallback](https://docs.spring.io/spring-integration/docs/current/api/org/springframework/integration/acks/AcknowledgmentCallback.html)
      interface.
      The class is compatible with its synchronous counterparty.
    - [MessageConsumerTemplate](src/main/kotlin/me/kpavlov/messaging/MessageConsumerTemplate.kt) - supports starting
      and stopping message consumption as a reaction for [AvailabilityChangeEvent][AvailabilityChangeEvent]

* [AWS SQS][sqs] Support
    - Reactive [Message Consumer](src/main/kotlin/me/kpavlov/messaging/sqs/consumer/SqsMessageConsumer.kt).
      It supports:
        - backpressure
        - rate limited sqs pooling
        - SQS batching
        - stopping and starting on [AvailabilityChangeEvent][AvailabilityChangeEvent]

    - [SqsPublisher](src/main/kotlin/me/kpavlov/messaging/sqs/publisher/SqsPublisher.kt)

[SpringIntegration]: (https://spring.io/projects/spring-integration),

[sqs]: (https://aws.amazon.com/sqs/),

[SpringMessaging]: (https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html)

[AvailabilityChangeEvent]: (https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/availability/AvailabilityChangeEvent.html)
