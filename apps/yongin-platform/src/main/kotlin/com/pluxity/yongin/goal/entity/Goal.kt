package com.pluxity.yongin.goal.entity

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
    name = "goal",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_goal_input_date_construction_section",
            columnNames = ["input_date", "construction_section_id"],
        ),
    ],
)
class Goal(
    @Column(name = "input_date", nullable = false)
    var inputDate: LocalDate,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_section_id", nullable = false)
    var constructionSection: ConstructionSection,
    @Column(name = "progress_rate")
    var progressRate: Float,
    @Column(name = "construction_rate")
    var constructionRate: Float,
    @Column(name = "total_quantity")
    var totalQuantity: Int,
    @Column(name = "cumulative_quantity")
    var cumulativeQuantity: Int,
    @Column(name = "previous_cumulative_quantity")
    var previousCumulativeQuantity: Int,
    @Column(name = "target_quantity")
    var targetQuantity: Int,
    @Column(name = "work_quantity")
    var workQuantity: Int,
    @Column(name = "start_date")
    var startDate: LocalDate,
    @Column(name = "completion_date")
    var completionDate: LocalDate,
    @Column(name = "planned_work_days")
    var plannedWorkDays: Int,
    @Column(name = "delay_days")
    var delayDays: Int,
    @Column(name = "is_active", nullable = false)
    @ColumnDefault("false")
    var isActive: Boolean = false,
) : IdentityIdEntity() {
    fun update(
        constructionSection: ConstructionSection,
        progressRate: Float,
        constructionRate: Float,
        totalQuantity: Int,
        cumulativeQuantity: Int,
        previousCumulativeQuantity: Int,
        targetQuantity: Int,
        workQuantity: Int,
        startDate: LocalDate,
        completionDate: LocalDate,
        plannedWorkDays: Int,
        delayDays: Int,
        isActive: Boolean,
    ) {
        this.constructionSection = constructionSection
        this.progressRate = progressRate
        this.constructionRate = constructionRate
        this.totalQuantity = totalQuantity
        this.cumulativeQuantity = cumulativeQuantity
        this.previousCumulativeQuantity = previousCumulativeQuantity
        this.targetQuantity = targetQuantity
        this.workQuantity = workQuantity
        this.startDate = startDate
        this.completionDate = completionDate
        this.plannedWorkDays = plannedWorkDays
        this.delayDays = delayDays
        this.isActive = isActive
    }
}
