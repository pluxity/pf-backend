package com.pluxity.safers.event.kafka

class RetryableException(
    message: String,
) : RuntimeException(message)
