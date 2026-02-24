package com.pluxity.common.file.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "zip_content_entries")
class ZipContentEntry(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    val file: FileEntity,
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "is_directory", nullable = false)
    val isDirectory: Boolean,
) : IdentityIdEntity()
