package com.pluxity.yonginplatform.cctv.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId

fun dummyCctv(
    id: Long? = null,
    streamName: String = "cam1",
    name: String? = null,
    lon: Double? = null,
    lat: Double? = null,
) = Cctv(
    streamName = streamName,
    name = name,
    lon = lon,
    lat = lat,
).withId(id).withAudit()

fun dummyCctvFavorite(
    id: Long? = null,
    streamName: String = "cam1",
    displayOrder: Int = 1,
) = CctvFavorite(
    streamName = streamName,
    displayOrder = displayOrder,
).withId(id).withAudit()
