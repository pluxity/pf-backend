package com.pluxity.safers.configuration.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId

fun dummyConfiguration(
    id: Long? = 1L,
    key: String = "WEATHER_API",
    value: String = "your-api-key",
): Configuration =
    Configuration(
        key = key,
        value = value,
    ).withId(id).withAudit()
