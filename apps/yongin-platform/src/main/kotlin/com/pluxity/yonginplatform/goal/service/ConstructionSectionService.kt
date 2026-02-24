package com.pluxity.yonginplatform.goal.service

import com.pluxity.common.core.exception.CustomException
import com.pluxity.yonginplatform.global.constant.YonginErrorCode
import com.pluxity.yonginplatform.goal.dto.ConstructionSectionRequest
import com.pluxity.yonginplatform.goal.dto.ConstructionSectionResponse
import com.pluxity.yonginplatform.goal.dto.toResponse
import com.pluxity.yonginplatform.goal.entity.ConstructionSection
import com.pluxity.yonginplatform.goal.repository.ConstructionSectionRepository
import com.pluxity.yonginplatform.goal.repository.GoalRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ConstructionSectionService(
    private val repository: ConstructionSectionRepository,
    private val goalRepository: GoalRepository,
) {
    fun findAll(): List<ConstructionSectionResponse> = repository.findAll().map { it.toResponse() }

    @Transactional
    fun create(request: ConstructionSectionRequest): Long = repository.save(ConstructionSection(name = request.name)).requiredId

    @Transactional
    fun delete(id: Long) {
        val constructionSection = getById(id)
        if (goalRepository.existsByConstructionSection(constructionSection)) {
            throw CustomException(YonginErrorCode.CONSTRUCTION_SECTION_HAS_GOAL)
        }
        repository.deleteById(constructionSection.requiredId)
    }

    fun getById(id: Long): ConstructionSection =
        repository.findByIdOrNull(id)
            ?: throw CustomException(YonginErrorCode.NOT_FOUND_CONSTRUCTION_SECTION, id)
}
