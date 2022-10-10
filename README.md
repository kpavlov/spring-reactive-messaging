# spring-reactive-messaging

Reactive producer/consumer components
inspired by [Spring Messaging](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html)
and [Spring Integration](https://spring.io/projects/spring-integration),
implemented using [Project Reactor](https://projectreactor.io/).

## Features

* AWS SQS Support
    - Reactive [Message Consumer](src/main/kotlin/me/kpavlov/messaging/sqs/consumer/SqsMessageConsumer.kt)
    - [SqsPublisher](src/main/kotlin/me/kpavlov/messaging/sqs/publisher/SqsPublisher.kt)
