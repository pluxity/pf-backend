package com.pluxity.safers.site.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.safers.site.entity.Site
import org.springframework.data.jpa.repository.JpaRepository

interface SiteRepository :
    JpaRepository<Site, Long>,
    KotlinJdslJpqlExecutor
