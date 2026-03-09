package com.pluxity.safers.site.repository

import com.pluxity.safers.site.entity.Site
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SiteCustomRepository {
    fun findAllOrderByIdDesc(pageable: Pageable): Page<Site>
}
