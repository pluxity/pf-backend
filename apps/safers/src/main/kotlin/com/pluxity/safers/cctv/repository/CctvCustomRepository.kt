package com.pluxity.safers.cctv.repository

import com.pluxity.safers.cctv.entity.Cctv

interface CctvCustomRepository {
    fun findAllWithSite(siteId: Long? = null): List<Cctv>
}
