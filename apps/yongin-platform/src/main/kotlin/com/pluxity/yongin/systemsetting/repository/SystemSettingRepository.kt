package com.pluxity.yongin.systemsetting.repository

import com.pluxity.yongin.systemsetting.entity.SystemSetting
import org.springframework.data.jpa.repository.JpaRepository

interface SystemSettingRepository : JpaRepository<SystemSetting, Long>
