package com.pluxity.weekly.teams.repository

import com.pluxity.weekly.teams.entity.TeamsConversation
import org.springframework.data.jpa.repository.JpaRepository

interface TeamsConversationRepository : JpaRepository<TeamsConversation, Long> {
    fun existsByUserId(userId: Long): Boolean

    fun findByUserId(userId: Long): TeamsConversation?
}
