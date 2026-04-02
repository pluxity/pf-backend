package com.pluxity.safers.cctv.service

import com.pluxity.safers.cctv.repository.CctvRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class CctvSiteCache(
    private val cctvRepository: CctvRepository,
) {
    private var streamToSiteId: Map<String, Long> = emptyMap()

    @PostConstruct
    fun init() {
        refresh()
    }

    fun refresh() {
        val newMap =
            cctvRepository
                .findAllWithSite()
                .associate { it.streamName to it.site.requiredId }
        streamToSiteId = newMap
        log.info { "CctvSiteCache 갱신 완료: ${newMap.size}건" }
    }

    fun getSiteIdByStreamName(streamName: String): Long? = streamToSiteId[streamName]
}
