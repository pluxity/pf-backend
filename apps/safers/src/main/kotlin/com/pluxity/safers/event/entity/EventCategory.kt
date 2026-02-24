package com.pluxity.safers.event.entity

enum class EventCategory(
    val displayName: String,
) {
    DETECTION("객체탐지"),
    ROI("영역/경계선 이벤트"),
}
