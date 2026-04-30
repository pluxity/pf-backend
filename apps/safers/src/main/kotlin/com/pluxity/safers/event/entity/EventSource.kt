package com.pluxity.safers.event.entity

enum class EventSource(
    val displayName: String,
) {
    GAS_SENSOR("유해가스 센서"),
    SOS_DEVICE("SOS 디바이스"),
    SMART_BAND("스마트밴드"),
}
