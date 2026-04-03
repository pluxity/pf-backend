package com.pluxity.safers.event.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.response.PageResponse
import com.pluxity.safers.cctv.service.CctvSiteCache
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.event.kafka.RetryableException
import com.pluxity.safers.event.repository.EventRepository
import com.pluxity.safers.llm.EventLlmClient
import org.springframework.stereotype.Component

@Component
class EventFacade(
    private val eventService: EventService,
    private val eventFileDownloadService: EventFileDownloadService,
    private val eventLlmClient: EventLlmClient,
    private val eventRepository: EventRepository,
    private val cctvSiteCache: CctvSiteCache,
) {
    fun create(request: EventCreateRequest) {
        val snapshotFileId = eventFileDownloadService.downloadAndInitiateUpload(request.snapshot)
        val siteId =
            cctvSiteCache.getSiteIdByStreamName(request.path)
                ?: throw IllegalArgumentException("CCTV 스트림(${request.path})에 매핑된 현장을 찾을 수 없습니다")
        eventService.create(request, snapshotFileId, siteId)
    }

    fun uploadVideoByEventId(
        eventId: String,
        videoUrl: String,
    ) {
        val event =
            eventRepository.findByEventId(eventId)
                ?: throw RetryableException("이벤트 미생성 상태, 재시도 필요: eventId=$eventId")
        if (event.videoFileId != null) return

        val videoFileId = eventFileDownloadService.downloadAndInitiateUpload(videoUrl)
        eventService.uploadVideoByEventId(eventId, videoFileId)
    }

    fun findById(id: Long): EventResponse = eventService.findById(id)

    fun findAll(
        request: PageSearchRequest,
        query: String? = null,
    ): PageResponse<EventResponse> {
        val criteria = query?.let { eventLlmClient.parseEventFilter(it) }
        return eventService.findAll(request, criteria)
    }
}
