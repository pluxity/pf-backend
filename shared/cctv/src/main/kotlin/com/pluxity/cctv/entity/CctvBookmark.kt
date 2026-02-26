package com.pluxity.cctv.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "cctv_bookmark",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_cctv_bookmark_stream_name", columnNames = ["stream_name"]),
    ],
)
class CctvBookmark(
    @Column(name = "stream_name", nullable = false)
    val streamName: String,
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,
) : IdentityIdEntity() {
    fun updateDisplayOrder(displayOrder: Int) {
        this.displayOrder = displayOrder
    }
}
