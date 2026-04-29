package com.pluxity.safers.collect.event

import com.pluxity.safers.event.dto.EventCreateRequest

data class CctvEventCollected(
    val request: EventCreateRequest,
)
