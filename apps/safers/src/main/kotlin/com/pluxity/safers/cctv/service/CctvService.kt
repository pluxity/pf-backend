package com.pluxity.safers.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.cctv.client.CctvApiClient
import com.pluxity.safers.cctv.config.CctvErrorCode
import com.pluxity.safers.cctv.dto.CctvResponse
import com.pluxity.safers.cctv.dto.CctvUpdateRequest
import com.pluxity.safers.cctv.dto.toResponse
import com.pluxity.safers.cctv.entity.Cctv
import com.pluxity.safers.cctv.repository.CctvRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CctvService(
    private val repository: CctvRepository,
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

    fun findAll(): List<CctvResponse> =
        repository
            .findAll()
            .sortedBy { it.name }
            .map { it.toResponse() }

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
