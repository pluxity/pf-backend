package com.pluxity.safers.event.entity

enum class EventType(
    val category: EventCategory,
    val displayName: String,
) {
    NO_HELMET(EventCategory.DETECTION, "헬멧 미착용"),
    HELMET(EventCategory.DETECTION, "헬멧 착용"),
    FALLEN_PERSON(EventCategory.DETECTION, "쓰러진 사람"),
    INTRUSION(EventCategory.ROI, "영역 침입"),
    EXIT(EventCategory.ROI, "영역 이탈"),
    LINE_CROSSING(EventCategory.ROI, "경계선 통과"),
}
