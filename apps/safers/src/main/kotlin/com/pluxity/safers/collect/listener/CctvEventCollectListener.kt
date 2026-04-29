package com.pluxity.safers.collect.listener

import com.pluxity.safers.collect.event.CctvEventCollected
import com.pluxity.safers.collect.event.CctvVideoCollected
import com.pluxity.safers.event.exception.RetryableException
import com.pluxity.safers.event.service.EventFacade
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * CCTV 수집 비동기 처리 — 기존 Kafka consumer 대체.
 * 컨트롤러는 즉시 202 응답하고, 실제 적재/영상 다운로드는 본 리스너가 [CctvEventCollectAsyncConfig.cctvCollectExecutor] 풀에서 처리.
 */
@Component
class CctvEventCollectListener(
    private val eventFacade: EventFacade,
) {
    @Async("cctvCollectExecutor")
    @EventListener
    fun onCctvEventCollected(event: CctvEventCollected) {
        logger.info { "CCTV 이벤트 소비: eventId=${event.request.eventId}" }
        runCatching { eventFacade.create(event.request) }
            .onFailure { logger.error(it) { "CCTV 이벤트 처리 실패: eventId=${event.request.eventId}" } }
    }

    @Async("cctvCollectExecutor")
    @EventListener
    fun onCctvVideoCollected(event: CctvVideoCollected) {
        logger.info { "CCTV 영상 소비: eventId=${event.eventId}" }
        try {
            eventFacade.uploadVideoByEventId(event.eventId, event.videoUrl)
        } catch (e: RetryableException) {
            logger.warn { "영상 소비 실패, 3초 후 재시도 예약: eventId=${event.eventId} — ${e.message}" }
            scheduleVideoRetry(event)
        } catch (e: Exception) {
            logger.error(e) { "CCTV 영상 처리 실패: eventId=${event.eventId}" }
        }
    }

    private fun scheduleVideoRetry(event: CctvVideoCollected) {
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute {
            try {
                eventFacade.uploadVideoByEventId(event.eventId, event.videoUrl)
                logger.info { "영상 재시도 성공: eventId=${event.eventId}" }
            } catch (e: Exception) {
                logger.error(e) { "영상 재시도 최종 실패: eventId=${event.eventId} — ${e.message}" }
            }
        }
    }
}
