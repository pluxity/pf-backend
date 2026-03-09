package com.pluxity.yongin.processstatus.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.common.core.utils.findPageNotNull
import com.pluxity.yongin.processstatus.entity.ProcessStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class ProcessStatusCustomRepositoryImpl(
    private val executor: KotlinJdslJpqlExecutor,
) : ProcessStatusCustomRepository {
    override fun findAllOrderByWorkDateDesc(pageable: Pageable): Page<ProcessStatus> =
        executor.findPageNotNull(pageable) {
            select(entity(ProcessStatus::class))
                .from(entity(ProcessStatus::class))
                .orderBy(path(ProcessStatus::workDate).desc())
        }
}
