package com.pluxity.safers.configuration.dto

fun dummyConfigurationRequest(
    key: String = "WEATHER_API",
    value: String = "your-api-key",
): ConfigurationRequest = ConfigurationRequest(key = key, value = value)

fun dummyConfigurationUpdateRequest(value: String = "new-api-key"): ConfigurationUpdateRequest = ConfigurationUpdateRequest(value = value)
