package com.pluxity.common.file.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import com.pluxity.common.file.constant.FileStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "files")
@EntityListeners(AuditingEntityListener::class)
class FileEntity(
    @Column(name = "file_path", nullable = false, unique = true)
    var filePath: String,
    @Column(name = "original_file_name", nullable = false)
    var originalFileName: String,
    @Column(name = "content_type", nullable = false)
    var contentType: String,
) : IdentityIdEntity() {
    @Column(name = "file_status", nullable = false)
    @Enumerated(EnumType.STRING)
    var fileStatus: FileStatus = FileStatus.TEMP

    fun makeComplete(filePath: String) {
        this.filePath = filePath
        this.fileStatus = FileStatus.COMPLETE
    }
}
