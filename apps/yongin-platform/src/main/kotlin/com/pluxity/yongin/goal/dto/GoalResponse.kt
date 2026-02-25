package com.pluxity.yongin.goal.dto

import com.pluxity.yongin.goal.entity.Goal
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "목표관리 응답")
data class GoalResponse(
    @field:Schema(description = "목표관리 ID", example = "1")
    val id: Long,
    @field:Schema(description = "입력일자", example = "2026-01-15")
    val inputDate: LocalDate,
    @field:Schema(description = "시공구간")
    val constructionSection: ConstructionSectionResponse,
    @field:Schema(description = "진행률", example = "75.5")
    val progressRate: Float,
    @field:Schema(description = "공정률", example = "80.0")
    val constructionRate: Float,
    @field:Schema(description = "전체량", example = "1000")
    val totalQuantity: Int,
    @field:Schema(description = "누계량", example = "755")
    val cumulativeQuantity: Int,
    @field:Schema(description = "전일누계량", example = "700")
    val previousCumulativeQuantity: Int,
    @field:Schema(description = "목표량", example = "800")
    val targetQuantity: Int,
    @field:Schema(description = "작업량", example = "50")
    val workQuantity: Int,
    @field:Schema(description = "착공일", example = "2026-01-01")
    val startDate: LocalDate,
    @field:Schema(description = "준공일", example = "2026-12-31")
    val completionDate: LocalDate,
    @field:Schema(description = "계획작업일", example = "365")
    val plannedWorkDays: Int,
    @field:Schema(description = "지연일", example = "5")
    val delayDays: Int,
    @field:Schema(description = "활성화 여부", example = "true")
    val isActive: Boolean,
)

fun Goal.toResponse(): GoalResponse =
    GoalResponse(
        id = this.requiredId,
        inputDate = this.inputDate,
        constructionSection = this.constructionSection.toResponse(),
        progressRate = this.progressRate,
        constructionRate = this.constructionRate,
        totalQuantity = this.totalQuantity,
        cumulativeQuantity = this.cumulativeQuantity,
        previousCumulativeQuantity = this.previousCumulativeQuantity,
        targetQuantity = this.targetQuantity,
        workQuantity = this.workQuantity,
        startDate = this.startDate,
        completionDate = this.completionDate,
        plannedWorkDays = this.plannedWorkDays,
        delayDays = this.delayDays,
        isActive = this.isActive,
    )
