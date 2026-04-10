package com.pluxity.safers.site.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.safers.site.dto.SiteSummary
import com.pluxity.safers.site.entity.Site
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class SiteCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : SiteCustomRepository {
    override fun findAllOrderByIdDesc(pageable: Pageable): Page<Site> =
        executor.findPageNotNull(pageable) {
            select(entity(Site::class))
                .from(entity(Site::class))
                .orderBy(path(Site::id).desc())
        }

    override fun findAllSummaries(): List<SiteSummary> =
        executor.findAllNotNull {
            selectNew<SiteSummary>(
                path(Site::id),
                path(Site::name),
            ).from(entity(Site::class))
        }
}
