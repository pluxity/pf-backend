package com.pluxity.yonginplatform.notice.repository

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.yonginplatform.notice.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository :
    JpaRepository<Notice, Long>,
    KotlinJdslJpqlExecutor
