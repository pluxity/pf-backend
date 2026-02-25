package com.pluxity.yongin.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.cctv.client.CctvApiClient
import com.pluxity.yongin.cctv.dto.CctvResponse
import com.pluxity.yongin.cctv.dto.CctvUpdateRequest
import com.pluxity.yongin.cctv.dto.toResponse
import com.pluxity.yongin.cctv.entity.Cctv
import com.pluxity.yongin.cctv.repository.CctvFavoriteRepository
import com.pluxity.yongin.cctv.repository.CctvRepository
import com.pluxity.yongin.global.constant.YonginErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CctvService(
    private val repository: CctvRepository,
    private val favoriteRepository: CctvFavoriteRepository,
    private val apiClient: CctvApiClient,
) {
    @Transactional
    fun sync() {
        val externalPaths = apiClient.fetchPaths()
        val externalPathNames = externalPaths.map { it.name }

        val existingCctvs = repository.findAll()
        val existingStreamNameMap = existingCctvs.associateBy { it.streamName }

        val newCctvs =
            externalPathNames
                .filter { it !in existingStreamNameMap }
                .map { Cctv(streamName = it) }
        if (newCctvs.isNotEmpty()) {
            repository.saveAll(newCctvs)
        }

        val externalPathSet = externalPathNames.toSet()
        val toDelete = existingCctvs.filter { it.streamName !in externalPathSet }
        if (toDelete.isNotEmpty()) {
            repository.deleteAllInBatch(toDelete)
        }
    }

    fun findAll(): List<CctvResponse> {
        val favorites = favoriteRepository.findAllByOrderByDisplayOrderAsc()
        val favoriteStreamNames = favorites.map { it.streamName }.toSet()

        val cctvMap = repository.findAll().associateBy { it.streamName }

        val favoriteCctvs = favoriteStreamNames.mapNotNull { cctvMap[it] }
        val nonFavoriteCctvs =
            cctvMap.values
                .filter { it.streamName !in favoriteStreamNames }
                .sortedBy { it.name }

        return (favoriteCctvs + nonFavoriteCctvs).map { it.toResponse() }
    }

    @Transactional
    fun update(
        id: Long,
        request: CctvUpdateRequest,
    ) {
        getById(id).update(
            name = request.name,
            lon = request.lon,
            lat = request.lat,
        )
    }

    private fun getById(id: Long): Cctv =
        repository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_CCTV, id)
}
