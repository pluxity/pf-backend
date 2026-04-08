package com.pluxity.safers.site.dto

import com.pluxity.safers.site.entity.Site
import java.io.Serializable

data class SiteSummary(
    val id: Long,
    val name: String,
) : Serializable

fun Site.toSummary(): SiteSummary =
    SiteSummary(
        id = requiredId,
        name = name,
    )
