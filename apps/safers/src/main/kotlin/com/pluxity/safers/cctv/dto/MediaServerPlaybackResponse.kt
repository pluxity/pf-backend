package com.pluxity.safers.cctv.dto

data class MediaServerPlaybackResponse(
    val sessionId: String,
    val pathName: String,
    val hlsUrl: String,
    val webrtcUrl: String,
    val expiresAt: String,
)
