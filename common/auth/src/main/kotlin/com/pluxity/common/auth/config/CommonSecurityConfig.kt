package com.pluxity.common.auth.config

import com.pluxity.common.auth.authentication.security.CustomUserDetails
import com.pluxity.common.auth.authentication.security.JwtAuthenticationFilter
import com.pluxity.common.auth.authentication.security.JwtProvider
import com.pluxity.common.auth.properties.CorsProperties
import com.pluxity.common.auth.properties.JwtProperties
import com.pluxity.common.auth.properties.UserProperties
import com.pluxity.common.auth.user.repository.UserRepository
import com.pluxity.common.core.constant.ErrorCode
import com.pluxity.common.core.exception.CustomException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

private val log = KotlinLogging.logger {}

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties::class, UserProperties::class, CorsProperties::class)
class CommonSecurityConfig(
    private val repository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val securityPermitConfigurer: SecurityPermitConfigurer?,
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        *buildPermitPaths(),
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET)
                    .permitAll()
                    .requestMatchers("/auth/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .also { http ->
                securityPermitConfigurer?.customFilters()?.forEach { registration ->
                    http.addFilterBefore(registration.filter, registration.beforeFilter)
                }
            }.sessionManagement { sessionManagement: SessionManagementConfigurer<HttpSecurity> ->
                sessionManagement.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS,
                )
            }

        return http.build()
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

    @Bean
    fun userDetailsService(): UserDetailsService =
        UserDetailsService { username: String ->
            repository
                .findByUsername(username)
                ?.let { CustomUserDetails(it) }
                ?: throw CustomException(ErrorCode.NOT_FOUND_USER, username)
        }

    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter = JwtAuthenticationFilter(jwtProvider, userDetailsService())

    private fun buildPermitPaths(): Array<String> {
        val defaultPaths =
            listOf(
                "/actuator/**",
                "/health",
                "/info",
                "/prometheus",
                "/error",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api-docs/**",
                "/swagger-config/**",
                "/docs/**",
            )
        val appPaths = securityPermitConfigurer?.permitPaths().orEmpty()
        return (defaultPaths + appPaths).toTypedArray()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val defaultPatterns =
            listOf(
                "http://localhost:*",
                "http://192.168.*.*:*",
                "https://*.pluxity.com",
            )

        val allPatterns = defaultPatterns + corsProperties.additionalOriginPatterns
        log.info { "CORS allowedOriginPatterns: $allPatterns" }

        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = allPatterns.toMutableList()
        configuration.allowedMethods = mutableListOf("GET", "PATCH", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = mutableListOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
