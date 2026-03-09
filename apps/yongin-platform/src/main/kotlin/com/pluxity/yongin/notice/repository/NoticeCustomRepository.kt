package com.pluxity.yongin.notice.repository

import com.pluxity.yongin.notice.entity.Notice
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface NoticeCustomRepository {
    fun findAllOrderByIdDesc(pageable: Pageable): Page<Notice>

    fun findAllActive(today: LocalDate): List<Notice>
}
