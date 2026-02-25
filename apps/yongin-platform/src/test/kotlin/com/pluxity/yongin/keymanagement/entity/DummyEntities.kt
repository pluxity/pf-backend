package com.pluxity.yongin.keymanagement.entity

import com.pluxity.common.core.test.withId

fun dummyKeyManagement(
    id: Long? = null,
    type: KeyManagementType = KeyManagementType.QUALITY,
    title: String = "테스트 제목",
    methodFeature: String? = "공법특징",
    methodContent: String? = "공법내용",
    methodDirection: String? = "공법추진방향",
    displayOrder: Int = 1,
    fileId: Long? = null,
    selected: Boolean = false,
) = KeyManagement(
    type = type,
    title = title,
    methodFeature = methodFeature,
    methodContent = methodContent,
    methodDirection = methodDirection,
    displayOrder = displayOrder,
    fileId = fileId,
    selected = selected,
).withId(id)
