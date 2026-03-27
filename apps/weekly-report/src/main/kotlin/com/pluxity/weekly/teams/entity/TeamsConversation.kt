package com.pluxity.weekly.teams.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "teams_conversation")
class TeamsConversation(
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "conversation_id", nullable = false)
    val conversationId: String,
    @Column(name = "service_url", nullable = false)
    val serviceUrl: String,
) : IdentityIdEntity()
