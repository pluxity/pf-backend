package com.pluxity.yongin.notice.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.notice.dto.NoticeRequest
import com.pluxity.yongin.notice.dto.NoticeResponse
import com.pluxity.yongin.notice.dto.toResponse
import com.pluxity.yongin.notice.entity.Notice
import com.pluxity.yongin.notice.repository.NoticeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class NoticeService(
    private val repository: NoticeRepository,
) {
    fun findAll(request: PageSearchRequest): PageResponse<NoticeResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)

        val page =
            repository.findPageNotNull(pageable) {
                select(entity(Notice::class))
                    .from(entity(Notice::class))
                    .orderBy(path(Notice::id).desc())
            }
        return page.toPageResponse { it.toResponse() }
    }

    fun findById(id: Long): NoticeResponse = getById(id).toResponse()

    fun findActive(): List<NoticeResponse> {
        val today = LocalDate.now()
        return repository
            .findAllNotNull {
                select(entity(Notice::class))
                    .from(entity(Notice::class))
                    .where(
                        and(
                            path(Notice::isVisible).equal(true),
                            or(
                                path(Notice::isAlways).equal(true),
                                and(
                                    or(
                                        path(Notice::startDate).isNull(),
                                        path(Notice::startDate).lessThanOrEqualTo(today),
                                    ),
                                    or(
                                        path(Notice::endDate).isNull(),
                                        path(Notice::endDate).greaterThanOrEqualTo(today),
                                    ),
                                ),
                            ),
                        ),
                    ).orderBy(path(Notice::id).desc())
            }.map { it.toResponse() }
    }

    @Transactional
    fun create(request: NoticeRequest): Long {
        validateDateRange(request)
        return repository
            .save(
                Notice(
                    title = request.title,
                    content = request.content,
                    isVisible = request.isVisible,
                    isAlways = request.isAlways,
                    startDate = request.startDate,
                    endDate = request.endDate,
                ),
            ).requiredId
    }

    @Transactional
    fun update(
        id: Long,
        request: NoticeRequest,
    ) {
        validateDateRange(request)
        getById(id).update(
            title = request.title,
            content = request.content,
            isVisible = request.isVisible,
            isAlways = request.isAlways,
            startDate = request.startDate,
            endDate = request.endDate,
        )
    }

    @Transactional
    fun delete(id: Long) {
        repository.deleteById(getById(id).requiredId)
    }

    private fun validateDateRange(request: NoticeRequest) {
        if (!request.isVisible || request.isAlways) return

        if (request.startDate == null && request.endDate == null) {
            throw CustomException(YonginErrorCode.INVALID_NOTICE_DATE_REQUIRED)
        }

        if (request.startDate != null && request.endDate != null && request.startDate.isAfter(request.endDate)) {
            throw CustomException(YonginErrorCode.INVALID_NOTICE_DATE_RANGE)
        }
    }

    private fun getById(id: Long): Notice =
        repository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_NOTICE, id)
}
