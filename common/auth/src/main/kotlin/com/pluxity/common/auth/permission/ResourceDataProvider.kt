package com.pluxity.common.auth.permission

import com.pluxity.common.auth.permission.dto.ResourceItemResponse

interface ResourceDataProvider {
    val resourceType: String

    fun findAllResources(): List<ResourceItemResponse>
}
