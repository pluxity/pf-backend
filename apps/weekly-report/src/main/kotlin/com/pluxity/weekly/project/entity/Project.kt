package com.pluxity.weekly.project.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "projects")
class Project(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "description", length = 1000)
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ProjectStatus = ProjectStatus.TODO,
    @Column(name = "start_date")
    var startDate: LocalDate? = null,
    @Column(name = "due_date")
    var dueDate: LocalDate? = null,
    @Column(name = "pm_id")
    var pmId: Long? = null,
) : IdentityIdEntity() {
    fun update(
        name: String,
        description: String?,
        status: ProjectStatus,
        startDate: LocalDate?,
        dueDate: LocalDate?,
        pmId: Long?,
    ) {
        this.name = name
        this.description = description
        this.status = status
        this.startDate = startDate
        this.dueDate = dueDate
        this.pmId = pmId
    }
}
