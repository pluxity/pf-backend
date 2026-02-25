package com.pluxity.yongin.cctv.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "cctv_stream")
class Cctv(
    @Column(name = "stream_name", nullable = false, unique = true)
    val streamName: String,
    @Column(name = "name")
    var name: String? = null,
    @Column(name = "lon")
    var lon: Double? = null,
    @Column(name = "lat")
    var lat: Double? = null,
) : IdentityIdEntity() {
    fun update(
        name: String?,
        lon: Double?,
        lat: Double?,
    ) {
        this.name = name
        this.lon = lon
        this.lat = lat
    }
}
