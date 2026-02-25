package com.pluxity.cctv.service

import com.pluxity.cctv.config.CctvErrorCode
import com.pluxity.cctv.config.CctvProperties
import com.pluxity.cctv.dto.CctvFavoriteOrderRequest
import com.pluxity.cctv.dto.CctvFavoriteRequest
import com.pluxity.cctv.dto.CctvFavoriteResponse
import com.pluxity.cctv.dto.toResponse
import com.pluxity.cctv.entity.CctvFavorite
import com.pluxity.cctv.repository.CctvFavoriteRepository
import com.pluxity.common.core.exception.CustomException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CctvFavoriteService(
    private val repository: CctvFavoriteRepository,
    private val cctvProperties: CctvProperties,
) {
    fun findAll(): List<CctvFavoriteResponse> = repository.findAllByOrderByDisplayOrderAsc().map { it.toResponse() }

    @Transactional
    fun create(request: CctvFavoriteRequest): Long {
        if (repository.existsByStreamName(request.streamName)) {
            throw CustomException(CctvErrorCode.ALREADY_FAVORITE, request.streamName)
        }
        val count = repository.count()
        if (count >= cctvProperties.maxFavoriteCount) {
            throw CustomException(CctvErrorCode.EXCEED_FAVORITE_LIMIT, cctvProperties.maxFavoriteCount)
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
                ?: throw CustomException(CctvErrorCode.NOT_FOUND_CCTV_FAVORITE, id)
        repository.delete(favorite)
    }

    @Transactional
    fun updateOrder(request: CctvFavoriteOrderRequest) {
        val totalCount = repository.count()
        if (request.ids.size.toLong() != totalCount) {
            throw CustomException(CctvErrorCode.INVALID_FAVORITE_ORDER_COUNT)
        }
        val favorites = repository.findAllById(request.ids).associateBy { it.requiredId }
        request.ids.forEachIndexed { index, id ->
            val favorite =
                favorites[id]
                    ?: throw CustomException(CctvErrorCode.NOT_FOUND_CCTV_FAVORITE, id)
            favorite.updateDisplayOrder(index + 1)
        }
    }
}
