package com.pluxity.safers.event.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.event.entity.EventType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

class EventCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : EventCustomRepository {
    override fun findAllByDateRange(
        pageable: Pageable,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        types: List<EventType>?,
    ): Page<Event> =
        executor.findPageNotNull(pageable) {
            select(entity(Event::class))
                .from(entity(Event::class))
                .whereAnd(
                    startDate?.let { path(Event::eventTimestamp).greaterThanOrEqualTo(it) },
                    endDate?.let { path(Event::eventTimestamp).lessThanOrEqualTo(it) },
                    types?.takeIf { it.isNotEmpty() }?.let { path(Event::type).`in`(it) },
                ).orderBy(path(Event::id).desc())
        }
}
