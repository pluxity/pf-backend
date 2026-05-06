package com.pluxity.safersCollect.forwarder.dto

data class TelemetryBatchRequest(
    val samples: List<TelemetryEnvelope>,
)
