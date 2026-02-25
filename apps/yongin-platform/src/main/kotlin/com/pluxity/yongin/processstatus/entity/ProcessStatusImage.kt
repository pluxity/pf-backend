package com.pluxity.yongin.processstatus.entity

import com.pluxity.common.core.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "process_status_image")
class ProcessStatusImage(
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = SINGLETON_ID,
    @Column(name = "file_id")
    var fileId: Long,
) : BaseEntity() {
    fun update(fileId: Long) {
        this.fileId = fileId
    }

    companion object {
        const val SINGLETON_ID = 1L
    }
}
