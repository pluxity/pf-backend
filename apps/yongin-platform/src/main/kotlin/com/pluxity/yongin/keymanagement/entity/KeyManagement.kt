package com.pluxity.yongin.keymanagement.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "key_management")
class KeyManagement(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: KeyManagementType,
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "method_feature", columnDefinition = "TEXT")
    var methodFeature: String?,
    @Column(name = "method_content", columnDefinition = "TEXT")
    var methodContent: String?,
    @Column(name = "method_direction", columnDefinition = "TEXT")
    var methodDirection: String?,
    @Column(name = "display_order")
    var displayOrder: Int,
    @Column(name = "file_id")
    var fileId: Long?,
    @Column(name = "selected")
    var selected: Boolean = false,
) : IdentityIdEntity() {
    fun update(
        type: KeyManagementType,
        title: String,
        methodFeature: String?,
        methodContent: String?,
        methodDirection: String?,
    ) {
        this.type = type
        this.title = title
        this.methodFeature = methodFeature
        this.methodContent = methodContent
        this.methodDirection = methodDirection
    }

    fun select() {
        this.selected = true
    }

    fun deselect() {
        this.selected = false
    }

    fun updateFileId(fileId: Long?) {
        this.fileId = fileId
    }
}
