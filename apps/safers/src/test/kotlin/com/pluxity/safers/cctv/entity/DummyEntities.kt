package com.pluxity.safers.cctv.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId
import com.pluxity.safers.site.entity.Site
import com.pluxity.safers.site.entity.dummySite

fun dummyCctv(
    id: Long? = null,
    site: Site = dummySite(),
    streamName: String = "cam1",
    name: String = streamName,
    lon: Double? = null,
    lat: Double? = null,
    alt: Double? = null,
    nvrName: String? = null,
    channel: Int? = null,
) = Cctv(
    site = site,
    streamName = streamName,
    name = name,
    lon = lon,
    lat = lat,
    alt = alt,
    nvrName = nvrName,
    channel = channel,
).withId(id).withAudit()
