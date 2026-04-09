package com.pluxity.safers.chat.dto

import com.fasterxml.jackson.annotation.JsonValue
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.event.dto.EventResponse

sealed class ActionResult {
    data class PaginatedEvent(
        val content: List<EventResponse>,
        val pageNumber: Int,
        val pageSize: Int,
        val totalElements: Long,
        val last: Boolean,
        val first: Boolean,
        val queryContext: QueryContext,
    ) : ActionResult()

    data class ListResult<T>(
        @JsonValue
        val data: List<T>,
    ) : ActionResult()

    data class SingleResult(
        @JsonValue
        val data: Any,
    ) : ActionResult()
}

data class QueryContext(
    val actionId: String,
    val target: QueryTarget,
    val filters: ActionFilter,
)

fun PageResponse<EventResponse>.toPaginatedEvent(queryContext: QueryContext) =
    ActionResult.PaginatedEvent(
        content = this.content,
        pageNumber = this.pageNumber,
        pageSize = this.pageSize,
        totalElements = this.totalElements,
        last = this.last,
        first = this.first,
        queryContext = queryContext,
    )
