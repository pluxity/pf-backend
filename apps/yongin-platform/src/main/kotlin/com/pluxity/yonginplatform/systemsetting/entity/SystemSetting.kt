package com.pluxity.yonginplatform.systemsetting.entity

import com.pluxity.common.core.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "system_settings")
class SystemSetting(
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = SINGLETON_ID,
    @Column(name = "rolling_interval_seconds", nullable = false)
    var rollingIntervalSeconds: Int,
    @Column(name = "bim_thumbnail_file_id")
    var bimThumbnailFileId: Long? = null,
    @Column(name = "aerial_view_file_id")
    var aerialViewFileId: Long? = null,
) : BaseEntity() {
    fun update(
        rollingIntervalSeconds: Int,
        bimThumbnailFileId: Long?,
        aerialViewFileId: Long?,
    ) {
        this.rollingIntervalSeconds = rollingIntervalSeconds
        this.bimThumbnailFileId = bimThumbnailFileId
        this.aerialViewFileId = aerialViewFileId
    }

    companion object {
        const val SINGLETON_ID = 1L
    }
}
