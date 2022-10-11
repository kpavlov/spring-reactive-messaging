# spring-reactive-messaging

[![Build with Maven](https://github.com/kpavlov/spring-reactive-messaging/actions/workflows/build.yaml/badge.svg)](https://github.com/kpavlov/spring-reactive-messaging/actions/workflows/build.yaml)

Reactive producer/consumer components
inspired by [Spring Messaging](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html)
and [Spring Integration](https://spring.io/projects/spring-integration),
implemented using [Project Reactor](https://projectreactor.io/).

## Motivation

[Spring Integration](https://spring.io/projects/spring-integration) provides a framework for connecting to messaging
systems,
but there is no support for [AWS SQS](https://aws.amazon.com/sqs/).
Also, [Spring Messaging](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html) has synchronous
API which is not designed
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
      and stopping message consumption as a reaction
      on [AvailabilityChangeEvent](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/availability/AvailabilityChangeEvent.html)

* [AWS SQS](https://aws.amazon.com/sqs/) Support
    - Reactive [Message Consumer](src/main/kotlin/me/kpavlov/messaging/sqs/consumer/SqsMessageConsumer.kt).
      It supports:
        - backpressure
        - rate limited sqs pooling
        - SQS batching
        - stopping and starting
          on [AvailabilityChangeEvent](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/availability/AvailabilityChangeEvent.html)

    - [SqsPublisher](src/main/kotlin/me/kpavlov/messaging/sqs/publisher/SqsPublisher.kt)

## Links

- ["Processing SQS Messages using Spring Boot and Project Reactor"](http://www.java-allandsundry.com/2020/03/processing-sqs-messages-using-spring.html)
- Even more simple SQS Listener: ["How to Setup a Reactive SQS Listener Using the AWS SDK and Spring Boot"](https://nickolasfisher.com/blog/How-to-Setup-a-Reactive-SQS-Listener-Using-the-AWS-SDK-and-Spring-Boot)
