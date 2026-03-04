package com.pluxity.safers.cctv.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.safers.cctv.client.CctvApiClient
import com.pluxity.safers.cctv.config.CctvErrorCode
import com.pluxity.safers.cctv.dto.CctvPlaybackRequest
import com.pluxity.safers.cctv.dto.CctvPlaybackResponse
import com.pluxity.safers.cctv.dto.CctvResponse
import com.pluxity.safers.cctv.dto.CctvUpdateRequest
import com.pluxity.safers.site.repository.SiteRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Component
class CctvFacade(
    private val cctvService: CctvService,
    private val siteRepository: SiteRepository,
    private val apiClient: CctvApiClient,
) {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }

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

        val sitePathsMap =
            runBlocking(Dispatchers.IO) {
                sites
                    .mapNotNull { site -> site.baseUrl?.let { site to it } }
                    .map { (site, baseUrl) ->
                        async {
                            try {
                                site to apiClient.fetchPaths(baseUrl, site.requiredId)
                            } catch (e: Exception) {
                                log.warn(e) { "Site ${site.requiredId}(${site.name})의 미디어서버($baseUrl) 경로 조회 실패" }
                                null
                            }
                        }
                    }.awaitAll()
                    .filterNotNull()
            }

        cctvService.syncAll(sitePathsMap)
    }

    fun findAll(siteId: Long? = null): List<CctvResponse> = cctvService.findAll(siteId)

    fun update(
        id: Long,
        request: CctvUpdateRequest,
    ) = cctvService.update(id, request)

    fun playback(
        cctvId: Long,
        request: CctvPlaybackRequest,
    ): CctvPlaybackResponse {
        val cctv = cctvService.findById(cctvId)
        val site = cctv.site

        val baseUrl =
            site.baseUrl
                ?: throw CustomException(CctvErrorCode.MISSING_BASE_URL, site.requiredId)
        val nvrId =
            cctv.nvrId
                ?: throw CustomException(CctvErrorCode.MISSING_NVR_INFO, cctvId)
        val channel =
            cctv.channel
                ?: throw CustomException(CctvErrorCode.MISSING_NVR_INFO, cctvId)

        val startTime = LocalDateTime.parse(request.startDate, DATE_FORMATTER)
        val endTime = LocalDateTime.parse(request.endDate, DATE_FORMATTER)

        val response = apiClient.requestPlayback(baseUrl, site.requiredId, nvrId, channel, startTime, endTime)
        return CctvPlaybackResponse(pathName = response.pathName)
    }
}
