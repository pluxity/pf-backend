package com.pluxity.safersCollect.forwarder.enums

// safers 모듈의 EventType (SAFETY 카테고리) 와 wire 호환. 값 이름이 곧 직렬화 형태.
enum class WireEventType {
    GAS_THRESHOLD_EXCEEDED,
    SOS_TRIGGERED,
    BAND_VITAL_ABNORMAL,
    BAND_FALL_DETECTED,
    BAND_OFFLINE,
}
