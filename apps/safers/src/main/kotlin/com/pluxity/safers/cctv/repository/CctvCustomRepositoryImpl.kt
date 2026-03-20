package com.pluxity.safers.cctv.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findAllNotNull
import com.pluxity.safers.cctv.entity.Cctv
import com.pluxity.safers.llm.dto.CctvFilterCriteria
import com.pluxity.safers.site.entity.Site

class CctvCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : CctvCustomRepository {
    override fun findAllWithSite(criteria: CctvFilterCriteria?): List<Cctv> =
        executor.findAllNotNull {
            select(entity(Cctv::class))
                .from(
                    entity(Cctv::class),
                    fetchJoin(Cctv::site),
                ).whereAnd(
                    criteria?.name?.let { upper(path(Cctv::name)).like("%${escapeLike(it.uppercase())}%") },
                    criteria?.siteIds?.takeIf { it.isNotEmpty() }?.let { path(Cctv::site)(Site::id).`in`(it) },
                ).orderBy(path(Cctv::name).asc())
        }

    private fun escapeLike(value: String): String = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
