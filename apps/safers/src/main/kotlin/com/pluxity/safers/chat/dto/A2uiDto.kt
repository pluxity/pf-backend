package com.pluxity.safers.chat.dto

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * A2UI Protocol v0.8 메시지 타입
 * 각 메시지는 정확히 하나의 필드만 포함해야 함
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class A2uiMessage(
    val surfaceUpdate: SurfaceUpdateMessage? = null,
    val dataModelUpdate: DataModelUpdateMessage? = null,
    val beginRendering: BeginRenderingMessage? = null,
    val deleteSurface: DeleteSurfaceMessage? = null,
)

data class SurfaceUpdateMessage(
    val surfaceId: String,
    val components: List<Map<String, Any>>,
)

data class DataModelUpdateMessage(
    val surfaceId: String,
    val path: String? = null,
    val contents: Any,
)

data class BeginRenderingMessage(
    val surfaceId: String,
    val root: String = "root",
    val catalogId: String? = null,
)

data class DeleteSurfaceMessage(
    val surfaceId: String,
)
