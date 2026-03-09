package com.pluxity.yongin.keymanagement.dto

import com.pluxity.common.file.dto.FileResponse
import com.pluxity.yongin.keymanagement.entity.KeyManagementType

fun dummyKeyManagementRequest(
    type: KeyManagementType = KeyManagementType.QUALITY,
    title: String = "테스트 제목",
    methodFeature: String? = "공법특징",
    methodContent: String? = "공법내용",
    methodDirection: String? = "공법추진방향",
    displayOrder: Int = 1,
    fileId: Long? = null,
) = KeyManagementRequest(
    type = type,
    title = title,
    methodFeature = methodFeature,
    methodContent = methodContent,
    methodDirection = methodDirection,
    displayOrder = displayOrder,
    fileId = fileId,
)

fun dummyKeyManagementUpdateRequest(
    type: KeyManagementType = KeyManagementType.QUALITY,
    title: String = "수정된 제목",
    methodFeature: String? = "공법특징",
    methodContent: String? = "공법내용",
    methodDirection: String? = "공법추진방향",
    fileId: Long? = null,
) = KeyManagementUpdateRequest(
    type = type,
    title = title,
    methodFeature = methodFeature,
    methodContent = methodContent,
    methodDirection = methodDirection,
    fileId = fileId,
)

fun dummyKeyManagementResponse(
    id: Long = 1L,
    type: KeyManagementType = KeyManagementType.QUALITY,
    title: String = "테스트 제목",
    methodFeature: String? = "공법특징",
    methodContent: String? = "공법내용",
    methodDirection: String? = "공법추진방향",
    displayOrder: Int = 1,
    fileId: Long? = null,
    file: FileResponse? = null,
    selected: Boolean = false,
) = KeyManagementResponse(
    id = id,
    type = type,
    title = title,
    methodFeature = methodFeature,
    methodContent = methodContent,
    methodDirection = methodDirection,
    displayOrder = displayOrder,
    fileId = fileId,
    file = file,
    selected = selected,
)

fun dummyKeyManagementGroupResponse(
    type: KeyManagementType = KeyManagementType.QUALITY,
    typeDescription: String = "품질",
    items: List<KeyManagementResponse> = listOf(dummyKeyManagementResponse()),
) = KeyManagementGroupResponse(
    type = type,
    typeDescription = typeDescription,
    items = items,
)
