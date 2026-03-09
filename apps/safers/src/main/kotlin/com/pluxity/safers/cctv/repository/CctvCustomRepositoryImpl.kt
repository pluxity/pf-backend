package com.pluxity.safers.cctv.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.safers.cctv.entity.Cctv
import com.pluxity.safers.site.entity.Site

class CctvCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : CctvCustomRepository {
    override fun findAllWithSite(siteId: Long?): List<Cctv> =
        executor.findAllNotNull {
            select(entity(Cctv::class))
                .from(
                    entity(Cctv::class),
                    fetchJoin(Cctv::site),
                ).whereAnd(
                    siteId?.let { path(Cctv::site)(Site::id).eq(it) },
                ).orderBy(path(Cctv::name).asc())
        }
}
