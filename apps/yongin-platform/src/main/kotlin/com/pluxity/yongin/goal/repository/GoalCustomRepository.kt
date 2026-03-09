package com.pluxity.yongin.goal.repository

import com.pluxity.yongin.goal.entity.Goal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GoalCustomRepository {
    fun findAllOrderByInputDateDesc(pageable: Pageable): Page<Goal>
}
