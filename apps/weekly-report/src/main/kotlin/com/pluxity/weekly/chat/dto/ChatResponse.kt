package com.pluxity.weekly.chat.dto

import com.pluxity.weekly.chat.action.dto.ActionResult

data class ChatResponse(
    val results: List<ActionResult>,
)
