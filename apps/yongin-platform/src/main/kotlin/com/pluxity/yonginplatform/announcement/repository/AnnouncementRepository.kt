package com.pluxity.yonginplatform.announcement.repository

import com.pluxity.yonginplatform.announcement.entity.Announcement
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementRepository : JpaRepository<Announcement, Long>
