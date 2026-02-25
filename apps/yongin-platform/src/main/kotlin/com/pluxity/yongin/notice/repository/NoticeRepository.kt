package com.pluxity.yongin.notice.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.yongin.notice.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository :
    JpaRepository<Notice, Long>,
    KotlinJdslJpqlExecutor
