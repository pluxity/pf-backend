package com.pluxity.safers.event.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.file.extensions.getFileMapByIds
import com.pluxity.common.file.service.FileService
import com.pluxity.safers.event.dto.EventCreateRequest
import com.pluxity.safers.event.dto.EventResponse
import com.pluxity.safers.event.dto.toResponse
import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.event.listener.EventCreated
import com.pluxity.safers.event.listener.EventVideoRegistered
import com.pluxity.safers.event.repository.EventRepository
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.llm.dto.EventFilterCriteria
import com.pluxity.safers.site.dto.toResponse
import com.pluxity.safers.site.repository.SiteRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class EventService(
    private val eventRepository: EventRepository,
    private val fileService: FileService,
    private val eventPublisher: ApplicationEventPublisher,
    private val siteRepository: SiteRepository,
) {
    companion object {
        private const val EVENT_PATH = "events/"
    }

    @Transactional
    fun create(
        request: EventCreateRequest,
        snapshotFileId: Long?,
        siteId: Long,
    ) {
        val event =
            Event(
                eventId = request.eventId,
                eventTimestamp = request.timestamp,
                category = request.category,
                type = request.type,
                trackId = request.trackId,
                name = request.name,
                bbox = request.bbox?.toString(),
                centerX = request.center?.x,
                centerY = request.center?.y,
                confidence = request.confidence,
                path = request.path,
                siteId = siteId,
            )

        val savedEvent = eventRepository.save(event)

        snapshotFileId?.let {
            fileService.finalizeUpload(it, "$EVENT_PATH${savedEvent.requiredId}/")
        }
        savedEvent.assignSnapshotFile(snapshotFileId)

        val fileResponse = fileService.getFileResponse(snapshotFileId)
        val siteResponse = siteRepository.findByIdOrNull(siteId)?.toResponse(null)
        eventPublisher.publishEvent(EventCreated(savedEvent.toResponse(fileResponse, siteResponse = siteResponse)))
    }

    @Transactional
    fun uploadVideoByEventId(
        eventId: String,
        videoFileId: Long?,
    ) {
        val event =
            eventRepository.findByEventId(eventId)
                ?: throw IllegalStateException("이벤트를 찾을 수 없습니다: eventId=$eventId")

        event.assignVideoFile(videoFileId)

        videoFileId?.let {
            fileService.finalizeUpload(it, "$EVENT_PATH${event.requiredId}/")
            val snapshotFileResponse = fileService.getFileResponse(event.snapshotFileId)
            val videoFileResponse = fileService.getFileResponse(it)
            val siteResponse = siteRepository.findByIdOrNull(event.siteId)?.toResponse(null)
            eventPublisher.publishEvent(EventVideoRegistered(event.toResponse(snapshotFileResponse, videoFileResponse, siteResponse)))
        }
    }

    fun findById(id: Long): EventResponse {
        val event =
            eventRepository.findByIdOrNull(id)
                ?: throw CustomException(SafersErrorCode.NOT_FOUND_EVENT, id)

        val snapshotFileResponse = fileService.getFileResponse(event.snapshotFileId)
        val videoFileResponse = fileService.getFileResponse(event.videoFileId)
        val siteResponse = siteRepository.findByIdOrNull(event.siteId)?.toResponse(null)
        return event.toResponse(snapshotFileResponse, videoFileResponse, siteResponse)
    }

    fun findAll(
        request: PageSearchRequest,
        criteria: EventFilterCriteria? = null,
    ): PageResponse<EventResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)
        val page = eventRepository.findAllByFilter(pageable, criteria)

        val fileMap = fileService.getFileMapByIds(page.content) { listOf(it.snapshotFileId, it.videoFileId) }
        val siteIds = page.content.map { it.siteId }.distinct()
        val siteMap = siteRepository.findAllById(siteIds).associateBy { it.requiredId }

        return page.toPageResponse {
            it.toResponse(
                fileMap[it.snapshotFileId],
                fileMap[it.videoFileId],
                siteMap[it.siteId]?.toResponse(null),
            )
        }
    }
}
