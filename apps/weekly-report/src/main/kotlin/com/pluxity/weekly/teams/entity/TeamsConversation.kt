package com.pluxity.weekly.teams.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "teams_conversation")
class TeamsConversation(
    @Column(name = "aad_object_id", nullable = false, unique = true)
    val aadObjectId: String,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "conversation_id", nullable = false)
    var conversationId: String,
    @Column(name = "service_url", nullable = false)
    var serviceUrl: String,
) : IdentityIdEntity() {
    fun update(
        serviceUrl: String,
        conversationId: String,
    ) {
        this.serviceUrl = serviceUrl
        this.conversationId = conversationId
    }
}
