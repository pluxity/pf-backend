package com.pluxity.yongin.global.config

import com.pluxity.common.core.config.ApiConfigurer
import com.pluxity.common.core.config.apiGroup
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YonginApiConfigurer : ApiConfigurer {
    override fun openApiInfo(): Info =
        Info()
            .title("Yongin Platform API")
            .description("Yongin Platform API Documentation")
            .version("1.0.0")

    @Bean
    fun processStatusApi(): GroupedOpenApi = apiGroup("5. 공정현황 관리 API", "/process-statuses/**")

    @Bean
    fun goalApi(): GroupedOpenApi = apiGroup("6. 목표 관리 API", "/goals/**")

    @Bean
    fun keyManagementApi(): GroupedOpenApi = apiGroup("7. 주요관리사항 관리 API", "/key-management/**")

    @Bean
    fun attendanceApi(): GroupedOpenApi = apiGroup("8. 출역현황 관리 API", "/attendances/**")

    @Bean
    fun cctvApi(): GroupedOpenApi = apiGroup("9. CCTV API", "/cctvs/**", "/cctv-bookmarks/**")

    @Bean
    fun safetyEquipmentApi(): GroupedOpenApi = apiGroup("10. 안전장비 API", "/safety-equipments/**")

    @Bean
    fun observationApi(): GroupedOpenApi = apiGroup("11. 관측 API", "/observations/**")

    @Bean
    fun noticeApi(): GroupedOpenApi = apiGroup("12. 공지사항 API", "/notices/**")

    @Bean
    fun announcementApi(): GroupedOpenApi = apiGroup("13. 안내방송 API", "/announcement/**")

    @Bean
    fun weatherApi(): GroupedOpenApi = apiGroup("14. 날씨 API", "/weather/**")

    @Bean
    fun systemSettingApi(): GroupedOpenApi = apiGroup("15. 시스템설정 API", "/system-settings/**")

    @Bean
    fun workerLocationApi(): GroupedOpenApi = apiGroup("16. 근로자 위치 API", "/worker-locations/**")
}
