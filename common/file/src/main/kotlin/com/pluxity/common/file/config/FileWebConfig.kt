package com.pluxity.common.file.config

import com.pluxity.common.file.properties.FileProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class FileWebConfig(
    private val fileProperties: FileProperties,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/files/**")
            .addResourceLocations("file:${fileProperties.local.path}/")
            .setCacheControl(CacheControl.noCache())
    }
}
