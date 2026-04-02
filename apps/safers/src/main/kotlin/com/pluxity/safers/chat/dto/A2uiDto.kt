package com.pluxity.safers.chat.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class A2uiMessage(
    val version: String = "v0.9",
    val createSurface: CreateSurface? = null,
    val updateComponents: UpdateComponents? = null,
    val updateDataModel: UpdateDataModel? = null,
)

data class CreateSurface(
    val surfaceId: String,
    val catalogId: String,
)

data class UpdateComponents(
    val surfaceId: String,
    val components: List<Map<String, Any>>,
)

data class UpdateDataModel(
    val surfaceId: String,
    val value: Any,
    val path: String? = null,
)
