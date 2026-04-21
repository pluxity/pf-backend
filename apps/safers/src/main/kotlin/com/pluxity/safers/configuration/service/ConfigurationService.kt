package com.pluxity.safers.configuration.service

import com.pluxity.safers.configuration.repository.ConfigurationRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class ConfigurationService(
    private val configurationRepository: ConfigurationRepository,
) {
    @Cacheable(value = [CONFIGURATIONS_CACHE], key = "#key", unless = "#result == null")
    fun findValue(key: String): String? = configurationRepository.findByKey(key)?.value

    companion object {
        const val CONFIGURATIONS_CACHE = "configurations"
    }
}
