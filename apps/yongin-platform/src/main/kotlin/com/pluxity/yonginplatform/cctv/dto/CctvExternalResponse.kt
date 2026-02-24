package com.pluxity.yonginplatform.cctv.dto

data class MediaServerPathListResponse(
    val itemCount: Int,
    val pageCount: Int,
    val items: List<MediaServerPathItem>,
)

data class MediaServerPathItem(
    val name: String,
    val confName: String,
    val ready: Boolean,
)
