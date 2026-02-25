package com.pluxity.yongin.announcement.entity

import com.pluxity.common.core.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "announcement")
class Announcement(
    @Id
    @Column(name = "id", nullable = false)
    val id: Long = SINGLETON_ID,
    @Column(name = "content", columnDefinition = "TEXT")
    var content: String,
) : BaseEntity() {
    fun update(content: String) {
        this.content = content
    }

    companion object {
        const val SINGLETON_ID = 1L
    }
}
