package com.pluxity.yongin.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.cctv.dto.CctvFavoriteOrderRequest
import com.pluxity.yongin.cctv.dto.CctvFavoriteRequest
import com.pluxity.yongin.cctv.dto.CctvFavoriteResponse
import com.pluxity.yongin.cctv.dto.toResponse
import com.pluxity.yongin.cctv.entity.CctvFavorite
import com.pluxity.yongin.cctv.repository.CctvFavoriteRepository
import com.pluxity.yongin.global.constant.YonginErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CctvFavoriteService(
    private val repository: CctvFavoriteRepository,
) {
    companion object {
        private const val MAX_FAVORITE_COUNT = 4
    }

    fun findAll(): List<CctvFavoriteResponse> = repository.findAllByOrderByDisplayOrderAsc().map { it.toResponse() }

    @Transactional
    fun create(request: CctvFavoriteRequest): Long {
        if (repository.existsByStreamName(request.streamName)) {
            throw CustomException(YonginErrorCode.ALREADY_FAVORITE, request.streamName)
        }
        val count = repository.count()
        if (count >= MAX_FAVORITE_COUNT) {
            throw CustomException(YonginErrorCode.EXCEED_FAVORITE_LIMIT)
        }
        return repository
            .save(
                CctvFavorite(
                    streamName = request.streamName,
                    displayOrder = (count + 1).toInt(),
                ),
            ).requiredId
    }

    @Transactional
    fun delete(id: Long) {
        val favorite =
            repository.findByIdOrNull(id)
                ?: throw CustomException(YonginErrorCode.NOT_FOUND_CCTV_FAVORITE, id)
        repository.delete(favorite)
    }

    @Transactional
    fun updateOrder(request: CctvFavoriteOrderRequest) {
        val totalCount = repository.count()
        if (request.ids.size.toLong() != totalCount) {
            throw CustomException(YonginErrorCode.INVALID_FAVORITE_ORDER_COUNT)
        }
        val favorites = repository.findAllById(request.ids).associateBy { it.requiredId }
        request.ids.forEachIndexed { index, id ->
            val favorite =
                favorites[id]
                    ?: throw CustomException(YonginErrorCode.NOT_FOUND_CCTV_FAVORITE, id)
            favorite.updateDisplayOrder(index + 1)
        }
    }
}
