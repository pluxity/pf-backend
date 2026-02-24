package com.pluxity.yonginplatform.processstatus.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.ColumnDefault
import java.time.LocalDate

@Entity
@Table(
    name = "process_status",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_process_status_work_date_work_type",
            columnNames = ["work_date", "work_type_id"],
        ),
    ],
)
class ProcessStatus(
    @Column(name = "work_date", nullable = false)
    var workDate: LocalDate,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id", nullable = false)
    var workType: WorkType,
    @Column(name = "planned_rate")
    var plannedRate: Int,
    @Column(name = "actual_rate")
    var actualRate: Int,
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("false")
    var isActive: Boolean = false,
) : IdentityIdEntity() {
    fun update(
        workDate: LocalDate,
        workType: WorkType,
        plannedRate: Int,
        actualRate: Int,
        isActive: Boolean,
    ) {
        this.workDate = workDate
        this.workType = workType
        this.plannedRate = plannedRate
        this.actualRate = actualRate
        this.isActive = isActive
    }
}
