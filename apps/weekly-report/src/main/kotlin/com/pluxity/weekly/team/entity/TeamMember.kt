package com.pluxity.weekly.team.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "team_members",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_team_member",
            columnNames = ["team_id", "user_id"],
        ),
    ],
)
class TeamMember(
    @Column(name = "team_id", nullable = false)
    val teamId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
) : IdentityIdEntity()
