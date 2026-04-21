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
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ConfigurationService(
    private val configurationRepository: ConfigurationRepository,
) {
    companion object {
        const val CONFIGURATIONS_CACHE = "configurations"
    }

    @Cacheable(value = [CONFIGURATIONS_CACHE], key = "#key", unless = "#result == null")
    fun findValue(key: String): String? = configurationRepository.findByKey(key)?.value

    @Transactional
    @CacheEvict(value = [CONFIGURATIONS_CACHE], key = "#request.key")
    fun create(request: ConfigurationRequest): String {
        if (configurationRepository.findByKey(request.key) != null) {
            throw CustomException(SafersErrorCode.DUPLICATE_CONFIGURATION, request.key)
        }
        return configurationRepository.save(Configuration(key = request.key, value = request.value)).key
    }

    fun findAll(request: PageSearchRequest): PageResponse<ConfigurationResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)
        return configurationRepository.findAll(pageable).toPageResponse { it.toResponse() }
    }

    fun findByKey(key: String): ConfigurationResponse = getConfigurationByKey(key).toResponse()

    @Transactional
    @CacheEvict(value = [CONFIGURATIONS_CACHE], key = "#key")
    fun update(
        key: String,
        request: ConfigurationUpdateRequest,
    ) {
        getConfigurationByKey(key).update(request.value)
    }

    @Transactional
    @CacheEvict(value = [CONFIGURATIONS_CACHE], key = "#key")
    fun delete(key: String) {
        configurationRepository.delete(getConfigurationByKey(key))
    }

    private fun getConfigurationByKey(key: String): Configuration =
        configurationRepository.findByKey(key)
            ?: throw CustomException(SafersErrorCode.NOT_FOUND_CONFIGURATION, key)
}
