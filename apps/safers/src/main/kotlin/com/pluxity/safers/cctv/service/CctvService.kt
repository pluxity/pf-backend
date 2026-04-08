package com.pluxity.safers.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.file.extensions.getFileMapById
import com.pluxity.common.file.service.FileService
import com.pluxity.safers.cctv.dto.CctvResponse
import com.pluxity.safers.cctv.dto.CctvSummary
import com.pluxity.safers.cctv.dto.CctvUpdateRequest
import com.pluxity.safers.cctv.dto.MediaServerPathItem
import com.pluxity.safers.cctv.dto.toResponse
import com.pluxity.safers.cctv.entity.Cctv
import com.pluxity.safers.cctv.repository.CctvRepository
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.llm.dto.CctvFilterCriteria
import com.pluxity.safers.site.entity.Site
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CctvService(
    private val repository: CctvRepository,
    private val fileService: FileService,
) {
    @CacheEvict("cctvs", allEntries = true)
    @Transactional
    fun syncAll(sitePathsMap: List<Pair<Site, List<MediaServerPathItem>>>) {
        sitePathsMap.forEach { (site, externalPaths) ->
            syncForSite(site, externalPaths)
        }
    }

    private fun syncForSite(
        site: Site,
        externalPaths: List<MediaServerPathItem>,
    ) {
        val externalPathMap = externalPaths.associateBy { it.name }

        val existingCctvList = repository.findBySiteId(site.requiredId)
        val existingStreamNameMap = existingCctvList.associateBy { it.streamName }

        val newCctvList =
            externalPaths
                .filter { it.name !in existingStreamNameMap }
                .map {
                    Cctv(
                        site = site,
                        streamName = it.name,
                        name = it.cctvName ?: it.name,
                        nvrId = it.nvrId,
                        channel = it.nvrChannel,
                    )
                }
        if (newCctvList.isNotEmpty()) {
            repository.saveAll(newCctvList)
        }

        existingCctvList.forEach { cctv ->
            externalPathMap[cctv.streamName]?.let { path ->
                cctv.name = path.cctvName ?: cctv.name
                cctv.nvrId = path.nvrId
                cctv.channel = path.nvrChannel
            }
        }

        val toDelete = existingCctvList.filter { it.streamName !in externalPathMap }
        if (toDelete.isNotEmpty()) {
            repository.deleteAllInBatch(toDelete)
        }
    }

    @Cacheable("cctvs")
    fun findAllSummaries(): List<CctvSummary> = repository.findAllSummaries()

    fun findAll(criteria: CctvFilterCriteria? = null): List<CctvResponse> {
        val cctvList = repository.findAllWithSite(criteria)

        val thumbnailFileMap = fileService.getFileMapById(cctvList) { it.site.thumbnailImageId }

        return cctvList.map { it.toResponse(thumbnailFileMap) }
    }

    @CacheEvict("cctvs", allEntries = true)
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

    fun findByIdWithSite(id: Long): Cctv =
        repository.findByIdWithSite(id)
            ?: throw CustomException(SafersErrorCode.NOT_FOUND_CCTV, id)

    private fun getById(id: Long): Cctv =
        repository.findByIdOrNull(id)
            ?: throw CustomException(SafersErrorCode.NOT_FOUND_CCTV, id)
}
