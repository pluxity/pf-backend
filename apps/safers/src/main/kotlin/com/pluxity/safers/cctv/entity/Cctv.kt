package com.pluxity.safers.cctv.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import com.pluxity.safers.site.entity.Site
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "cctv_stream")
class Cctv(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: Site,
    @Column(name = "stream_name", nullable = false, unique = true)
    val streamName: String,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "lon")
    var lon: Double? = null,
    @Column(name = "lat")
    var lat: Double? = null,
    @Column(name = "alt")
    var alt: Double? = null,
    @Column(name = "nvr_id")
    var nvrId: String? = null,
    @Column(name = "channel")
    var channel: Int? = null,
) : IdentityIdEntity() {
    fun update(
        name: String,
        lon: Double?,
        lat: Double?,
        alt: Double?,
    ) {
        this.name = name
        this.lon = lon
        this.lat = lat
        this.alt = alt
    }
}
