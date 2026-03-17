package com.pluxity.weekly.epic.entity

import com.pluxity.common.auth.user.entity.User
import com.pluxity.common.core.entity.IdentityIdEntity
import com.pluxity.weekly.project.entity.Project
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "epics")
class Epic(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "description", length = 1000)
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: EpicStatus = EpicStatus.TODO,
    @Column(name = "start_date")
    var startDate: LocalDate? = null,
    @Column(name = "due_date")
    var dueDate: LocalDate? = null,
) : IdentityIdEntity() {
    @OneToMany(mappedBy = "epic", cascade = [CascadeType.ALL], orphanRemoval = true)
    val assignments: MutableList<EpicAssignment> = mutableListOf()

    fun assign(user: User) {
        if (assignments.none { it.assignedBy == user }) {
            assignments.add(EpicAssignment(epic = this, assignedBy = user))
        }
    }

    fun unassign(user: User) {
        assignments.removeIf { it.assignedBy == user }
    }

    fun update(
        project: Project,
        name: String,
        description: String?,
        status: EpicStatus,
        startDate: LocalDate?,
        dueDate: LocalDate?,
    ) {
        this.project = project
        this.name = name
        this.description = description
        this.status = status
        this.startDate = startDate
        this.dueDate = dueDate
    }
}
