package com.pluxity.safers.site.dto

import com.pluxity.safers.site.entity.Region

data class RegionResponse(
    val name: String,
    val displayName: String,
)

fun Region.toResponse(): RegionResponse =
    RegionResponse(
        name = name,
        displayName = displayName,
    )
