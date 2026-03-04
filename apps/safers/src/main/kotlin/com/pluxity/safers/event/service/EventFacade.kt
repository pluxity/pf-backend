package com.pluxity.safers.event.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.dto.EventResponse
import org.springframework.stereotype.Component

@Component
class EventFacade(
    private val eventService: EventService,
    private val eventFileDownloadService: EventFileDownloadService,
) {
    fun create(request: EventCreateRequest): Long {
        val snapshotFileId = eventFileDownloadService.downloadAndInitiateUpload(request.snapshot)
        return eventService.create(request, snapshotFileId)
    }

    fun uploadVideo(
        eventId: Long,
        videoUrl: String,
    ) {
        val videoFileId = eventFileDownloadService.downloadAndInitiateUpload(videoUrl)
        eventService.uploadVideo(eventId, videoFileId)
    }

    fun findById(id: Long): EventResponse = eventService.findById(id)

    fun findAll(
        request: PageSearchRequest,
        startDate: String? = null,
        endDate: String? = null,
    ): PageResponse<EventResponse> = eventService.findAll(request, startDate, endDate)
}
