package com.pluxity.safers.site.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.locationtech.jts.geom.Polygon
import java.time.LocalDate

@Entity
@Table(name = "sites")
class Site(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "construction_start_date")
    var constructionStartDate: LocalDate? = null,
    @Column(name = "construction_end_date")
    var constructionEndDate: LocalDate? = null,
    @Column(name = "description")
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "region")
    var region: Region? = null,
    @Column(name = "address")
    var address: String? = null,
    @Column(name = "thumbnail_image_id")
    var thumbnailImageId: Long? = null,
    @Column(name = "location", columnDefinition = "geometry(Polygon, 4326)")
    var location: Polygon,
    @Column(name = "base_url")
    var baseUrl: String? = null,
    @Column(name = "nx", nullable = false)
    var nx: Int,
    @Column(name = "ny", nullable = false)
    var ny: Int,
) : IdentityIdEntity() {
    fun update(
        name: String,
        constructionStartDate: LocalDate?,
        constructionEndDate: LocalDate?,
        description: String?,
        region: Region?,
        address: String?,
        thumbnailImageId: Long?,
        location: Polygon,
        nx: Int,
        ny: Int,
        baseUrl: String?,
    ) {
        this.name = name
        this.constructionStartDate = constructionStartDate
        this.constructionEndDate = constructionEndDate
        this.description = description
        this.region = region
        this.address = address
        this.thumbnailImageId = thumbnailImageId
        this.location = location
        this.nx = nx
        this.ny = ny
        this.baseUrl = baseUrl
    }
}
