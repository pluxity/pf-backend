package com.pluxity.safers.event.entity

enum class EventSeverity(
    val displayName: String,
) {
    INFO("정보"),
    WARNING("경고"),
    CRITICAL("심각"),
}
