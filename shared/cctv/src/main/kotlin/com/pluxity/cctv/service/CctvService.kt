package com.pluxity.cctv.service

import com.pluxity.cctv.client.CctvApiClient
import com.pluxity.cctv.config.CctvErrorCode
import com.pluxity.cctv.dto.CctvResponse
import com.pluxity.cctv.dto.CctvUpdateRequest
import com.pluxity.cctv.dto.toResponse
import com.pluxity.cctv.entity.Cctv
import com.pluxity.cctv.repository.CctvFavoriteRepository
import com.pluxity.cctv.repository.CctvRepository
import com.pluxity.common.core.exception.CustomException
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

        val existingCctvList = repository.findAll()
        val existingStreamNameMap = existingCctvList.associateBy { it.streamName }

        val newCctvList =
            externalPathNames
                .filter { it !in existingStreamNameMap }
                .map { Cctv(streamName = it, name = it) }
        if (newCctvList.isNotEmpty()) {
            repository.saveAll(newCctvList)
        }

        val externalPathSet = externalPathNames.toSet()
        val toDelete = existingCctvList.filter { it.streamName !in externalPathSet }
        if (toDelete.isNotEmpty()) {
            repository.deleteAllInBatch(toDelete)
        }
    }

    fun findAll(): List<CctvResponse> {
        val favorites = favoriteRepository.findAllByOrderByDisplayOrderAsc()
        val favoriteStreamNames = favorites.map { it.streamName }.toSet()

        val cctvMap = repository.findAll().associateBy { it.streamName }

        val favoriteCctvList = favoriteStreamNames.mapNotNull { cctvMap[it] }
        val nonFavoriteCctvList =
            cctvMap.values
                .filter { it.streamName !in favoriteStreamNames }
                .sortedBy { it.name }

        return (favoriteCctvList + nonFavoriteCctvList).map { it.toResponse() }
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
            alt = request.alt,
        )
    }

    private fun getById(id: Long): Cctv =
        repository.findByIdOrNull(id)
            ?: throw CustomException(CctvErrorCode.NOT_FOUND_CCTV, id)
}
