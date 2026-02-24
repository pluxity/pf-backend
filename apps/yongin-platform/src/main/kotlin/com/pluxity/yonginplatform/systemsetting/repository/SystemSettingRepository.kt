package com.pluxity.yonginplatform.systemsetting.repository

import com.pluxity.yonginplatform.systemsetting.entity.SystemSetting
import org.springframework.data.jpa.repository.JpaRepository

interface SystemSettingRepository : JpaRepository<SystemSetting, Long>
