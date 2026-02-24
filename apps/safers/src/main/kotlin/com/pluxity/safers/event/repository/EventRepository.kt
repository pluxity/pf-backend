package com.pluxity.safers.event.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.safers.event.entity.Event
import org.springframework.data.jpa.repository.JpaRepository

interface EventRepository :
    JpaRepository<Event, Long>,
    KotlinJdslJpqlExecutor
