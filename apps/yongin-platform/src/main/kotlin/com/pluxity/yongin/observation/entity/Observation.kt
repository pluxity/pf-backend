package com.pluxity.yongin.observation.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "observation")
class Observation(
    @Column(name = "date", nullable = false)
    var date: LocalDate,
    @Column(name = "description")
    var description: String?,
    @Column(name = "file_id", nullable = false)
    var fileId: Long,
    @Column(name = "root_file_name", nullable = false)
    var rootFileName: String,
    @Column(name = "directory_path")
    var directoryPath: String? = null,
) : IdentityIdEntity() {
    fun update(
        date: LocalDate,
        description: String?,
        fileId: Long,
        rootFileName: String,
    ) {
        this.date = date
        this.description = description
        this.fileId = fileId
        this.rootFileName = rootFileName
    }

    fun updateDirectoryPath(directoryPath: String) {
        this.directoryPath = directoryPath
    }
}
