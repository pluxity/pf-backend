package com.pluxity.safers.site.repository

import com.pluxity.safers.site.entity.Site
import org.springframework.data.jpa.repository.JpaRepository

interface SiteRepository :
    JpaRepository<Site, Long>,
    SiteCustomRepository
