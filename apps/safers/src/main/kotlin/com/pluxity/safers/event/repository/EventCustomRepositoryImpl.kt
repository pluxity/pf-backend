package com.pluxity.safers.event.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.llm.dto.EventFilterCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class EventCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : EventCustomRepository {
    override fun findAllByFilter(
        pageable: Pageable,
        criteria: EventFilterCriteria?,
    ): Page<Event> =
        executor.findPageNotNull(pageable) {
            select(entity(Event::class))
                .from(entity(Event::class))
                .whereAnd(
                    criteria?.startDate?.let { path(Event::eventTimestamp).greaterThanOrEqualTo(it) },
                    criteria?.endDate?.let { path(Event::eventTimestamp).lessThanOrEqualTo(it) },
                    criteria?.types?.takeIf { it.isNotEmpty() }?.let { path(Event::type).`in`(it) },
                    criteria?.siteIds?.takeIf { it.isNotEmpty() }?.let { path(Event::siteId).`in`(it) },
                ).orderBy(path(Event::id).desc())
        }
}
