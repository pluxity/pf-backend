package com.pluxity.cctv.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "cctv_favorite",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cctv_favorite_stream_name", columnNames = ["stream_name"]),
    ],
)
class CctvFavorite(
    @Column(name = "stream_name", nullable = false)
    val streamName: String,
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,
) : IdentityIdEntity() {
    fun updateDisplayOrder(displayOrder: Int) {
        this.displayOrder = displayOrder
    }
}
