package com.pluxity.yongin.keymanagement.dto

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
