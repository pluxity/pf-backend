package com.pluxity.yongin.processstatus.repository

import com.pluxity.yongin.processstatus.entity.ProcessStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProcessStatusCustomRepository {
    fun findAllOrderByWorkDateDesc(pageable: Pageable): Page<ProcessStatus>
}
