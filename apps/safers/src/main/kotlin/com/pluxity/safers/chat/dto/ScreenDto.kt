package com.pluxity.safers.chat.dto

/**
 * 캐시된 화면 메타데이터 (프롬프트에 포함용)
 */
data class ScreenMeta(
    val ref: String,
    val summary: String,
    val siteIds: List<Long>,
    val targets: List<String>,
    val actionIds: List<String>,
)

/**
 * 캐시된 화면 전체 (복원용)
 */
data class ScreenCache(
    val meta: ScreenMeta,
    val actions: List<QueryAction>,
    val response: ChatResponse,
)
