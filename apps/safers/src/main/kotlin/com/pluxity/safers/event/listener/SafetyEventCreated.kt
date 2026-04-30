package com.pluxity.safers.event.listener

import com.pluxity.safers.ingest.dto.SafetyEventResponse

data class SafetyEventCreated(
    val eventResponse: SafetyEventResponse,
)
