package com.pluxity.safers.configuration.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.safers.configuration.dto.ConfigurationRequest
import com.pluxity.safers.configuration.dto.ConfigurationResponse
import com.pluxity.safers.configuration.dto.ConfigurationUpdateRequest
import com.pluxity.safers.configuration.dto.toResponse
import com.pluxity.safers.configuration.entity.Configuration
import com.pluxity.safers.configuration.repository.ConfigurationRepository
import com.pluxity.safers.global.constant.SafersErrorCode
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ConfigurationService(
    private val configurationRepository: ConfigurationRepository,
    private val cacheManager: CacheManager,
) {
    companion object {
        const val CONFIGURATIONS_CACHE = "configurations"
    }

    @Cacheable(value = [CONFIGURATIONS_CACHE], key = "#key", unless = "#result == null")
    fun findValue(key: String): String? = configurationRepository.findByKey(key)?.value

    @Transactional
    fun create(request: ConfigurationRequest): Long {
        if (configurationRepository.findByKey(request.key) != null) {
            throw CustomException(SafersErrorCode.DUPLICATE_CONFIGURATION, request.key)
        }
        val saved = configurationRepository.save(Configuration(key = request.key, value = request.value))
        evictCache(saved.key)
        return saved.requiredId
    }

    fun findAll(request: PageSearchRequest): PageResponse<ConfigurationResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)
        return configurationRepository.findAll(pageable).toPageResponse { it.toResponse() }
    }

    fun findById(id: Long): ConfigurationResponse = getConfigurationById(id).toResponse()

    @Transactional
    fun update(
        id: Long,
        request: ConfigurationUpdateRequest,
    ) {
        val configuration = getConfigurationById(id)
        configuration.update(request.value)
        evictCache(configuration.key)
    }

    @Transactional
    fun delete(id: Long) {
        val configuration = getConfigurationById(id)
        configurationRepository.delete(configuration)
        evictCache(configuration.key)
    }

    private fun getConfigurationById(id: Long): Configuration =
        configurationRepository.findByIdOrNull(id)
            ?: throw CustomException(SafersErrorCode.NOT_FOUND_CONFIGURATION_BY_ID, id)

    private fun evictCache(key: String) {
        cacheManager.getCache(CONFIGURATIONS_CACHE)?.evict(key)
    }
}
