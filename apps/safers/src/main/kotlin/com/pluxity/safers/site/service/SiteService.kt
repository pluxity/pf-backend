package com.pluxity.safers.site.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.common.file.extensions.getFileMapById
import com.pluxity.common.file.service.FileService
import com.pluxity.safers.global.constant.SafersErrorCode
import com.pluxity.safers.site.dto.SiteRequest
import com.pluxity.safers.site.dto.SiteResponse
import com.pluxity.safers.site.dto.toResponse
import com.pluxity.safers.site.entity.Site
import com.pluxity.safers.site.repository.SiteRepository
import com.pluxity.safers.weather.util.GridConverter
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.ParseException
import org.locationtech.jts.io.WKTReader
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SiteService(
    private val siteRepository: SiteRepository,
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private val fileService: FileService,
) {
    companion object {
        private const val SITE_PATH = "sites/"
        private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
    }

    @Transactional
    fun create(request: SiteRequest): Long {
        val polygon = parsePolygon(request.location)
        val centroid = polygon.centroid
        val (nx, ny) = GridConverter.toGrid(centroid.x, centroid.y)

        val site =
            Site(
                name = request.name,
                constructionStartDate = request.constructionStartDate,
                constructionEndDate = request.constructionEndDate,
                description = request.description,
                region = request.region,
                address = request.address,
                thumbnailImageId = request.thumbnailImageId,
                location = polygon,
                nx = nx,
                ny = ny,
            )

        val savedSite = siteRepository.save(site)

        request.thumbnailImageId?.let {
            fileService.finalizeUpload(it, "$SITE_PATH${savedSite.requiredId}/")
        }

        return savedSite.requiredId
    }

    fun findAll(request: PageSearchRequest): PageResponse<SiteResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)
        val page =
            siteRepository.findPageNotNull(pageable) {
                select(entity(Site::class))
                    .from(entity(Site::class))
                    .orderBy(path(Site::id).desc())
            }

        val fileMap = fileService.getFileMapById(page.content) { it.thumbnailImageId }
        return page.toPageResponse {
            it.toResponse(fileMap[it.thumbnailImageId])
        }
    }

    fun findById(id: Long): SiteResponse {
        val site = getSiteById(id)

        val thumbnailFileResponse = fileService.getFileResponse(site.thumbnailImageId)
        return site.toResponse(thumbnailFileResponse)
    }

    @Transactional
    fun update(
        id: Long,
        request: SiteRequest,
    ) {
        val site = getSiteById(id)

        val polygon = parsePolygon(request.location)
        val centroid = polygon.centroid
        val (nx, ny) = GridConverter.toGrid(centroid.x, centroid.y)

        val oldThumbnailImageId = site.thumbnailImageId

        site.update(
            name = request.name,
            constructionStartDate = request.constructionStartDate,
            constructionEndDate = request.constructionEndDate,
            description = request.description,
            region = request.region,
            address = request.address,
            thumbnailImageId = request.thumbnailImageId,
            location = polygon,
            nx = nx,
            ny = ny,
        )

        if (request.thumbnailImageId != null && request.thumbnailImageId != oldThumbnailImageId) {
            fileService.finalizeUpload(request.thumbnailImageId, "$SITE_PATH${site.requiredId}/")
        }
    }

    @Transactional
    fun delete(id: Long) {
        val site = getSiteById(id)
        siteRepository.delete(site)
    }

    private fun getSiteById(id: Long): Site =
        siteRepository.findByIdOrNull(id)
            ?: throw CustomException(SafersErrorCode.NOT_FOUND_SITE, id)

    private fun parsePolygon(wkt: String): Polygon {
        val geometry =
            try {
                WKTReader(geometryFactory).read(wkt)
            } catch (_: ParseException) {
                throw CustomException(SafersErrorCode.INVALID_LOCATION)
            }
        if (geometry !is Polygon) {
            throw CustomException(SafersErrorCode.INVALID_LOCATION)
        }
        geometry.srid = 4326
        return geometry
    }
}
