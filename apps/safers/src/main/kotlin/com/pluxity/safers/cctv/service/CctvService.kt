package com.pluxity.safers.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.common.file.extensions.getFileMapById
import com.pluxity.common.file.service.FileService
import com.pluxity.safers.cctv.client.CctvApiClient
import com.pluxity.safers.cctv.config.CctvErrorCode
import com.pluxity.safers.cctv.dto.CctvResponse
import com.pluxity.safers.cctv.dto.CctvUpdateRequest
import com.pluxity.safers.cctv.dto.MediaServerPathItem
import com.pluxity.safers.cctv.dto.toResponse
import com.pluxity.safers.cctv.entity.Cctv
import com.pluxity.safers.cctv.repository.CctvRepository
import com.pluxity.safers.site.entity.Site
import com.pluxity.safers.site.repository.SiteRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.ConcurrentHashMap

@Service
@Transactional(readOnly = true)
class CctvService(
    private val repository: CctvRepository,
    private val siteRepository: SiteRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val fileService: FileService,
    private val apiClient: CctvApiClient,
    private val transactionTemplate: TransactionTemplate,
) {
    fun sync(siteId: Long? = null) {
        val sites =
            if (siteId != null) {
                val site =
                    siteRepository.findByIdOrNull(siteId)
                        ?: throw CustomException(CctvErrorCode.NOT_FOUND_SITE, siteId)
                listOf(site)
            } else {
                siteRepository.findAll()
            }

        val siteBaseUrlMap = sites.mapNotNull { site -> site.baseUrl?.let { site to it } }

        val sitePathsMap = ConcurrentHashMap<Site, List<MediaServerPathItem>>()
        siteBaseUrlMap.parallelStream().forEach { (site, baseUrl) ->
            sitePathsMap[site] = apiClient.fetchPaths(baseUrl)
        }

        transactionTemplate.executeWithoutResult {
            sitePathsMap.forEach { (site, externalPaths) ->
                syncForSite(site, externalPaths)
            }
        }
    }

    private fun syncForSite(
        site: Site,
        externalPaths: List<MediaServerPathItem>,
    ) {
        val externalPathNames = externalPaths.map { it.name }

        val existingCctvList = repository.findBySiteId(site.requiredId)
        val existingStreamNameMap = existingCctvList.associateBy { it.streamName }

        val newCctvList =
            externalPathNames
                .filter { it !in existingStreamNameMap }
                .map { Cctv(site = site, streamName = it, name = it) }
        if (newCctvList.isNotEmpty()) {
            repository.saveAll(newCctvList)
        }

        val externalPathSet = externalPathNames.toSet()
        val toDelete = existingCctvList.filter { it.streamName !in externalPathSet }
        if (toDelete.isNotEmpty()) {
            repository.deleteAllInBatch(toDelete)
        }
    }

    fun findAll(siteId: Long? = null): List<CctvResponse> {
        val cctvList =
            repository.findAllNotNull {
                select(entity(Cctv::class))
                    .from(
                        entity(Cctv::class),
                        fetchJoin(Cctv::site),
                    ).whereAnd(
                        siteId?.let { path(Cctv::site)(Site::id).eq(it) },
                    ).orderBy(path(Cctv::name).asc())
            }

        val thumbnailFileMap = fileService.getFileMapById(cctvList) { it.site.thumbnailImageId }

        return cctvList.map { it.toResponse(thumbnailFileMap) }
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
