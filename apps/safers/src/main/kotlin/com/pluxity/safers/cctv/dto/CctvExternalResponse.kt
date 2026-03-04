package com.pluxity.safers.cctv.dto

data class MediaServerPathListResponse(
    val itemCount: Int,
    val pageCount: Int,
    val items: List<MediaServerPathItem>,
)

data class MediaServerPathItem(
    val name: String,
    val nvrId: String? = null,
    val nvrChannel: Int? = null,
    val cctvName: String? = null,
)
