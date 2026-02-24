package com.pluxity.common.auth.authentication.repository

import com.pluxity.common.auth.authentication.entity.RefreshToken
import org.springframework.data.repository.CrudRepository

interface RefreshTokenRepository : CrudRepository<RefreshToken, String> {
    fun findByToken(token: String): RefreshToken?
}
