package com.pluxity.yongin.goal.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.goal.entity.Goal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class GoalCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : GoalCustomRepository {
    override fun findAllOrderByInputDateDesc(pageable: Pageable): Page<Goal> =
        executor.findPageNotNull(pageable) {
            select(entity(Goal::class))
                .from(entity(Goal::class))
                .orderBy(path(Goal::inputDate).desc())
        }
}
