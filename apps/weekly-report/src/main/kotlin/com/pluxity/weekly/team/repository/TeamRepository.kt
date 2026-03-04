package com.pluxity.weekly.team.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.weekly.team.entity.Team
import org.springframework.data.jpa.repository.JpaRepository

interface TeamRepository :
    JpaRepository<Team, Long>,
    KotlinJdslJpqlExecutor
