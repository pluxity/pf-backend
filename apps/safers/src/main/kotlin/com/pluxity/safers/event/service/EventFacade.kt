package com.pluxity.safers.event.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.llm.LlmClient
import com.pluxity.safers.llm.LlmProvider
import org.springframework.stereotype.Component

@Component
class EventFacade(
    private val eventService: EventService,
    private val eventFileDownloadService: EventFileDownloadService,
    private val llmClient: LlmClient,
) {
    fun create(request: EventCreateRequest): Long {
        val snapshotFileId = eventFileDownloadService.downloadAndInitiateUpload(request.snapshot)
        return eventService.create(request, snapshotFileId)
    }

    fun uploadVideoByEventId(
        eventId: String,
        videoUrl: String,
    ) {
        val videoFileId = eventFileDownloadService.downloadAndInitiateUpload(videoUrl)
        eventService.uploadVideoByEventId(eventId, videoFileId)
    }

    fun findById(id: Long): EventResponse = eventService.findById(id)

    fun findAll(
        request: PageSearchRequest,
        query: String? = null,
        provider: LlmProvider = LlmProvider.OLLAMA,
    ): PageResponse<EventResponse> {
        val criteria = query?.let { llmClient.parseEventFilter(it, provider) }
        return eventService.findAll(request, criteria)
    }
}
