package com.pluxity.weekly.task.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import com.pluxity.weekly.epic.entity.Epic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "tasks")
class Task(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id", nullable = false)
    var epic: Epic,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "description", length = 1000)
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TaskStatus = TaskStatus.TODO,
    @Column(name = "progress", nullable = false)
    var progress: Int = 0,
    @Column(name = "start_date")
    var startDate: LocalDate? = null,
    @Column(name = "due_date")
    var dueDate: LocalDate? = null,
) : IdentityIdEntity() {
    fun update(
        epic: Epic,
        name: String,
        description: String?,
        status: TaskStatus,
        progress: Int,
        startDate: LocalDate?,
        dueDate: LocalDate?,
    ) {
        this.epic = epic
        this.name = name
        this.description = description
        this.status = status
        this.progress = progress
        this.startDate = startDate
        this.dueDate = dueDate
    }
}
