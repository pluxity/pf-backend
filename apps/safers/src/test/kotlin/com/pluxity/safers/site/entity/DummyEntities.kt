package com.pluxity.safers.site.entity

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader
import java.time.LocalDate

private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

fun dummyPolygon(wkt: String = "POLYGON ((126.96 37.55, 126.98 37.55, 126.98 37.56, 126.96 37.56, 126.96 37.55))"): Polygon {
    val geometry = WKTReader(geometryFactory).read(wkt) as Polygon
    geometry.srid = 4326
    return geometry
}

fun dummySite(
    id: Long? = 1L,
    name: String = "서울역 현장",
    constructionStartDate: LocalDate? = LocalDate.of(2024, 1, 1),
    constructionEndDate: LocalDate? = LocalDate.of(2025, 12, 31),
    description: String? = "서울역 리모델링 현장",
    region: Region? = Region.SEOUL,
    address: String? = "서울특별시 용산구 한강대로 405",
    baseUrl: String? = "https://example.com/api",
    thumbnailImageId: Long? = null,
    location: Polygon = dummyPolygon(),
    nx: Int = 55,
    ny: Int = 127,
): Site =
    Site(
        name = name,
        constructionStartDate = constructionStartDate,
        constructionEndDate = constructionEndDate,
        description = description,
        region = region,
        address = address,
        thumbnailImageId = thumbnailImageId,
        location = location,
        nx = nx,
        ny = ny,
        baseUrl = baseUrl,
    ).withId(id).withAudit()
