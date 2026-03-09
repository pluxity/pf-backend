package com.pluxity.yongin.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.cctv.config.CctvProperties
import com.pluxity.yongin.cctv.dto.CctvBookmarkOrderRequest
import com.pluxity.yongin.cctv.dto.CctvBookmarkRequest
import com.pluxity.yongin.cctv.dto.CctvBookmarkResponse
import com.pluxity.yongin.cctv.dto.toResponse
import com.pluxity.yongin.cctv.entity.CctvBookmark
import com.pluxity.yongin.cctv.repository.CctvBookmarkRepository
import com.pluxity.yongin.global.constant.YonginErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CctvBookmarkService(
    private val repository: CctvBookmarkRepository,
    private val cctvProperties: CctvProperties,
) {
    fun findAll(): List<CctvBookmarkResponse> = repository.findAllByOrderByDisplayOrderAsc().map { it.toResponse() }

    @Transactional
    fun create(request: CctvBookmarkRequest): Long {
        if (repository.existsByStreamName(request.streamName)) {
            throw CustomException(YonginErrorCode.ALREADY_BOOKMARK, request.streamName)
        }
        val count = repository.count()
        if (count >= cctvProperties.maxBookmarkCount) {
            throw CustomException(YonginErrorCode.EXCEED_BOOKMARK_LIMIT, cctvProperties.maxBookmarkCount)
        }
        return repository
            .save(
                CctvBookmark(
                    streamName = request.streamName,
                    displayOrder = (count + 1).toInt(),
                ),
            ).requiredId
    }

    @Transactional
    fun delete(id: Long) {
        val bookmark =
            repository.findByIdOrNull(id)
                ?: throw CustomException(YonginErrorCode.NOT_FOUND_CCTV_BOOKMARK, id)
        repository.delete(bookmark)
    }

    @Transactional
    fun updateOrder(request: CctvBookmarkOrderRequest) {
        val totalCount = repository.count()
        if (request.ids.size.toLong() != totalCount) {
            throw CustomException(YonginErrorCode.INVALID_BOOKMARK_ORDER_COUNT)
        }
        val bookmarks = repository.findAllById(request.ids).associateBy { it.requiredId }
        request.ids.forEachIndexed { index, id ->
            val bookmark =
                bookmarks[id]
                    ?: throw CustomException(YonginErrorCode.NOT_FOUND_CCTV_BOOKMARK, id)
            bookmark.updateDisplayOrder(index + 1)
        }
    }
}
