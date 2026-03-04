package com.pluxity.safers.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.common.file.extensions.getFileMapById
import com.pluxity.common.file.service.FileService
import com.pluxity.safers.cctv.config.CctvErrorCode
import com.pluxity.safers.cctv.dto.CctvResponse
import com.pluxity.safers.cctv.dto.CctvUpdateRequest
import com.pluxity.safers.cctv.dto.MediaServerPathItem
import com.pluxity.safers.cctv.dto.toResponse
import com.pluxity.safers.cctv.entity.Cctv
import com.pluxity.safers.cctv.repository.CctvRepository
import com.pluxity.safers.site.entity.Site
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CctvService(
    private val repository: CctvRepository,
    private val fileService: FileService,
) {
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
                        nvrName = it.nvrName,
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
                cctv.nvrName = path.nvrName
                cctv.channel = path.nvrChannel
            }
        }

        val toDelete = existingCctvList.filter { it.streamName !in externalPathMap }
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
