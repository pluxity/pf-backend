package com.pluxity.safers.configuration.repository

import com.pluxity.safers.configuration.entity.Configuration
import org.springframework.data.jpa.repository.JpaRepository

interface ConfigurationRepository : JpaRepository<Configuration, Long> {
    fun findByKey(key: String): Configuration?
}
