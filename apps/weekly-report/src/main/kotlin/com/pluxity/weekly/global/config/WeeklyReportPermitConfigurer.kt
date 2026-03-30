package com.pluxity.weekly.global.config

import com.pluxity.common.auth.authentication.security.JwtAuthenticationFilter
import com.pluxity.common.auth.config.SecurityFilterRegistration
import com.pluxity.common.auth.config.SecurityPermitConfigurer
import com.pluxity.weekly.teams.config.TeamsAuthFilter
import org.springframework.context.annotation.Configuration

@Configuration
class WeeklyReportPermitConfigurer(
    private val teamsAuthFilter: TeamsAuthFilter,
) : SecurityPermitConfigurer {
    override fun permitPaths(): List<String> = listOf("/teams/messages")

    override fun customFilters(): List<SecurityFilterRegistration> =
        listOf(
            SecurityFilterRegistration(
                filter = teamsAuthFilter,
                beforeFilter = JwtAuthenticationFilter::class.java,
            ),
        )
}
