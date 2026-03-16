package com.pluxity.weekly.chat.dto

data class ChatActionResponse(
    val action: String,
    val target: String,
    val id: Long? = null,
    val dto: ChatDto? = null,
    val beforeAction: List<BeforeAction>? = null,
)

data class BeforeAction(
    val field: String,
    val candidates: List<Candidate>,
)