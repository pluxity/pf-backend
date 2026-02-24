package com.pluxity.yonginplatform.attendance.client

import com.pluxity.common.core.config.WebClientFactory
import com.pluxity.yonginplatform.attendance.dto.AttendanceExternalData
import com.pluxity.yonginplatform.attendance.dto.AttendanceExternalResponse
import com.pluxity.yonginplatform.global.properties.AttendanceProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private val log = KotlinLogging.logger {}

@Component
class AttendanceApiClient(
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    webClientFactory: WebClientFactory,
    private val attendanceProperties: AttendanceProperties,
) {
    private val client: WebClient = webClientFactory.createClient(attendanceProperties.url)

    fun fetchAttendanceData(): List<AttendanceExternalData> {
        if (attendanceProperties.url.isBlank()) {
            log.warn { "Attendance API URL is not configured" }
            return emptyList()
        }

        val response =
            client
                .get()
                .retrieve()
                .bodyToMono<AttendanceExternalResponse>()
                .block()
        return response?.let { response.data } ?: emptyList()
    }
}
