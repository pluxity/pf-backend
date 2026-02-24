package com.pluxity.yonginplatform.announcement.service

import com.pluxity.yonginplatform.announcement.dto.AnnouncementRequest
import com.pluxity.yonginplatform.announcement.dto.AnnouncementResponse
import com.pluxity.yonginplatform.announcement.dto.toResponse
import com.pluxity.yonginplatform.announcement.entity.Announcement
import com.pluxity.yonginplatform.announcement.repository.AnnouncementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AnnouncementService(
    private val repository: AnnouncementRepository,
) {
    fun getAnnouncement(): AnnouncementResponse =
        repository.findByIdOrNull(Announcement.SINGLETON_ID)?.toResponse() ?: AnnouncementResponse()

    @Transactional
    fun saveAnnouncement(request: AnnouncementRequest) {
        repository
            .findByIdOrNull(Announcement.SINGLETON_ID)
            ?.apply { update(request.content) }
            ?: repository.save(Announcement(content = request.content))
    }
}
