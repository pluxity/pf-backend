package com.pluxity.safers.configuration.service

import com.pluxity.safers.configuration.repository.ConfigurationRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ConfigurationService(
    private val configurationRepository: ConfigurationRepository,
) {
    @Cacheable(value = [CACHE_NAME], unless = "#result == null")
    fun findValue(key: String): String? = configurationRepository.findByKey(key)?.value

    companion object {
        const val CACHE_NAME = "configurations"
    }
}
