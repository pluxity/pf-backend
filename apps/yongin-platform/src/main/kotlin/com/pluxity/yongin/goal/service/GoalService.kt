package com.pluxity.yongin.goal.service

import com.pluxity.common.core.dto.PageSearchRequest
import com.pluxity.common.core.exception.CustomException
import com.pluxity.common.core.response.PageResponse
import com.pluxity.common.core.response.toPageResponse
import com.pluxity.common.core.utils.findAllByIdsOrThrow
import com.pluxity.yongin.global.constant.YonginErrorCode
import com.pluxity.yongin.goal.dto.GoalBulkRequest
import com.pluxity.yongin.goal.dto.GoalResponse
import com.pluxity.yongin.goal.dto.toResponse
import com.pluxity.yongin.goal.entity.Goal
import com.pluxity.yongin.goal.repository.ConstructionSectionRepository
import com.pluxity.yongin.goal.repository.GoalRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GoalService(
    private val repository: GoalRepository,
    private val constructionSectionRepository: ConstructionSectionRepository,
) {
    fun findAll(request: PageSearchRequest): PageResponse<GoalResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size)

        val page = repository.findAllOrderByInputDateDesc(pageable)
        return page.toPageResponse { it.toResponse() }
    }

    fun findLatest(): List<GoalResponse> = repository.findAllByLatestInputDate().map { it.toResponse() }

    @Transactional
    fun saveOrUpdateAll(request: GoalBulkRequest) {
        // Delete
        if (request.deletedIds.isNotEmpty()) {
            repository.deleteAllById(request.deletedIds)
        }

        if (request.upserts.isEmpty()) return

        // ConstructionSection 한번에 조회
        val constructionSectionIds = request.upserts.map { it.constructionSectionId }.distinct()
        val constructionSectionMap =
            findAllByIdsOrThrow(
                ids = constructionSectionIds,
                findAllById = constructionSectionRepository::findAllById,
                idExtractor = { it.requiredId },
                errorCode = YonginErrorCode.NOT_FOUND_CONSTRUCTION_SECTION,
            )

        // 수정할 Goal 한번에 조회
        val updateIds = request.upserts.mapNotNull { it.id }
        val goalMap =
            findAllByIdsOrThrow(
                ids = updateIds,
                findAllById = repository::findAllById,
                idExtractor = { it.requiredId },
                errorCode = YonginErrorCode.NOT_FOUND_GOAL,
            )

        // Upsert
        request.upserts.forEach { item ->
            val constructionSection =
                constructionSectionMap[item.constructionSectionId]
                    ?: throw CustomException(YonginErrorCode.NOT_FOUND_CONSTRUCTION_SECTION, item.constructionSectionId)

            if (item.id == null) {
                repository.save(
                    Goal(
                        inputDate = item.inputDate,
                        constructionSection = constructionSection,
                        progressRate = item.progressRate,
                        constructionRate = item.constructionRate,
                        totalQuantity = item.totalQuantity,
                        cumulativeQuantity = item.cumulativeQuantity,
                        previousCumulativeQuantity = item.previousCumulativeQuantity,
                        targetQuantity = item.targetQuantity,
                        workQuantity = item.workQuantity,
                        startDate = item.startDate,
                        completionDate = item.completionDate,
                        plannedWorkDays = item.plannedWorkDays,
                        delayDays = item.delayDays,
                        isActive = item.isActive,
                    ),
                )
                return@forEach
            }

            (goalMap[item.id] ?: throw CustomException(YonginErrorCode.NOT_FOUND_GOAL, item.id))
                .update(
                    constructionSection = constructionSection,
                    progressRate = item.progressRate,
                    constructionRate = item.constructionRate,
                    totalQuantity = item.totalQuantity,
                    cumulativeQuantity = item.cumulativeQuantity,
                    previousCumulativeQuantity = item.previousCumulativeQuantity,
                    targetQuantity = item.targetQuantity,
                    workQuantity = item.workQuantity,
                    startDate = item.startDate,
                    completionDate = item.completionDate,
                    plannedWorkDays = item.plannedWorkDays,
                    delayDays = item.delayDays,
                    isActive = item.isActive,
                )
        }
    }
}
