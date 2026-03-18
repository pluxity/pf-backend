package com.pluxity.safers.event.repository

import com.pluxity.safers.event.entity.Event
import com.pluxity.safers.llm.dto.EventFilterCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EventCustomRepository {
    fun findAllByFilter(
        pageable: Pageable,
        criteria: EventFilterCriteria?,
    ): Page<Event>
}
