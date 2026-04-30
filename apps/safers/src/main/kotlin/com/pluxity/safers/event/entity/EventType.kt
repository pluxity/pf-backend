package com.pluxity.safers.event.entity

enum class EventType(
    val category: EventCategory,
    val displayName: String,
) {
    NO_HELMET(EventCategory.DETECTION, "헬멧 미착용"),
    HELMET(EventCategory.DETECTION, "헬멧 착용"),
    FALLEN_PERSON(EventCategory.DETECTION, "쓰러진 사람"),
    FIRE(EventCategory.DETECTION, "화재 감지"),
    SMOKE(EventCategory.DETECTION, "연기 감지"),
    INTRUSION(EventCategory.ROI, "영역 침입"),
    EXIT(EventCategory.ROI, "영역 이탈"),
    LINE_CROSSING(EventCategory.ROI, "경계선 통과"),
    GAS_THRESHOLD_EXCEEDED(EventCategory.SAFETY, "가스 임계 초과"),
    SOS_TRIGGERED(EventCategory.SAFETY, "SOS 긴급호출"),
    BAND_VITAL_ABNORMAL(EventCategory.SAFETY, "밴드 생체신호 이상"),
    BAND_FALL_DETECTED(EventCategory.SAFETY, "밴드 낙상 감지"),
    BAND_OFFLINE(EventCategory.SAFETY, "밴드 통신두절"),
}
