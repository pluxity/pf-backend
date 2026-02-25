package com.pluxity.yongin.goal.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.yongin.goal.entity.ConstructionSection
import com.pluxity.yongin.goal.entity.Goal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GoalRepository :
    JpaRepository<Goal, Long>,
    KotlinJdslJpqlExecutor {
    @Query("SELECT g FROM Goal g WHERE g.inputDate = (SELECT MAX(g2.inputDate) FROM Goal g2)")
    fun findAllByLatestInputDate(): List<Goal>

    fun existsByConstructionSection(constructionSection: ConstructionSection): Boolean
}
