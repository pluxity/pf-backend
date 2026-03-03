package com.pluxity.yongin.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yongin.cctv.client.CctvApiClient
import com.pluxity.yongin.cctv.config.CctvErrorCode
import com.pluxity.yongin.cctv.dto.CctvResponse
import com.pluxity.yongin.cctv.dto.CctvUpdateRequest
import com.pluxity.yongin.cctv.dto.toResponse
import com.pluxity.yongin.cctv.entity.Cctv
import com.pluxity.yongin.cctv.repository.CctvBookmarkRepository
import com.pluxity.yongin.cctv.repository.CctvRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CctvService(
    private val repository: CctvRepository,
    private val bookmarkRepository: CctvBookmarkRepository,
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
        val bookmarks = bookmarkRepository.findAllByOrderByDisplayOrderAsc()
        val bookmarkStreamNames = bookmarks.map { it.streamName }.toSet()

        val cctvMap = repository.findAll().associateBy { it.streamName }

        val bookmarkedCctvList = bookmarkStreamNames.mapNotNull { cctvMap[it] }
        val nonBookmarkedCctvList =
            cctvMap.values
                .filter { it.streamName !in bookmarkStreamNames }
                .sortedBy { it.name }

        return (bookmarkedCctvList + nonBookmarkedCctvList).map { it.toResponse() }
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
