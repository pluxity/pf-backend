package com.pluxity.safers.event.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findPageNotNull
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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val fileService: FileService,
    private val eventFileDownloadService: EventFileDownloadService,
    private val eventPublisher: ApplicationEventPublisher,
    private val transactionTemplate: TransactionTemplate,
) {
    companion object {
        private const val EVENT_PATH = "events/"
    }

    fun create(request: EventCreateRequest): Long {
        val snapshotFileId = eventFileDownloadService.downloadAndInitiateUpload("/snapshots/", request.snapshot)

        return transactionTemplate.execute {
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
                )

            val savedEvent = eventRepository.save(event)

            snapshotFileId?.let {
                fileService.finalizeUpload(it, "$EVENT_PATH${savedEvent.requiredId}/")
            }
            savedEvent.assignSnapshotFile(snapshotFileId)

            val fileResponse = fileService.getFileResponse(snapshotFileId)
            eventPublisher.publishEvent(EventCreated(savedEvent.toResponse(fileResponse)))

            savedEvent.requiredId
        }
    }

    fun uploadVideo(
        eventId: Long,
        videoFileName: String,
    ) {
        val videoFileId = eventFileDownloadService.downloadAndInitiateUpload("/videos/", videoFileName)

        transactionTemplate.executeWithoutResult {
            val event =
                eventRepository.findByIdOrNull(eventId)
                    ?: throw CustomException(SafersErrorCode.NOT_FOUND_EVENT, eventId)

            event.assignVideoFile(videoFileId)

            videoFileId?.let {
                fileService.finalizeUpload(it, "$EVENT_PATH$eventId/")
                val snapshotFileResponse = fileService.getFileResponse(event.snapshotFileId)
                val videoFileResponse = fileService.getFileResponse(it)
                eventPublisher.publishEvent(EventVideoRegistered(event.toResponse(snapshotFileResponse, videoFileResponse)))
            }
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): EventResponse {
        val event =
            eventRepository.findByIdOrNull(id)
                ?: throw CustomException(SafersErrorCode.NOT_FOUND_EVENT, id)

        val snapshotFileResponse = fileService.getFileResponse(event.snapshotFileId)
        val videoFileResponse = fileService.getFileResponse(event.videoFileId)
        return event.toResponse(snapshotFileResponse, videoFileResponse)
    }

    @Transactional(readOnly = true)
    fun findAll(request: PageSearchRequest): PageResponse<EventResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)
        val page =
            eventRepository.findPageNotNull(pageable) {
                select(entity(Event::class))
                    .from(entity(Event::class))
                    .orderBy(path(Event::id).desc())
            }

        val fileMap = fileService.getFileMapByIds(page.content) { listOf(it.snapshotFileId, it.videoFileId) }
        return page.toPageResponse {
            it.toResponse(
                fileMap[it.snapshotFileId],
                fileMap[it.videoFileId],
            )
        }
    }
}
