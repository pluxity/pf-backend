package com.pluxity.safers.event.repository

import com.pluxity.safers.event.entity.Event
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface EventCustomRepository {
    fun findAllByDateRange(
        pageable: Pageable,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
    ): Page<Event>
}
