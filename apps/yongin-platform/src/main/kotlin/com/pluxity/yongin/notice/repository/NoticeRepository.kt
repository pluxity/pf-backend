package com.pluxity.yongin.notice.repository

import com.pluxity.yongin.notice.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository :
    JpaRepository<Notice, Long>,
    NoticeCustomRepository
