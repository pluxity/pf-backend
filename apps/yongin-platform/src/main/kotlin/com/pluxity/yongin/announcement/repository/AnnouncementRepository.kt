package com.pluxity.yongin.announcement.repository

import com.pluxity.yongin.announcement.entity.Announcement
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementRepository : JpaRepository<Announcement, Long>
