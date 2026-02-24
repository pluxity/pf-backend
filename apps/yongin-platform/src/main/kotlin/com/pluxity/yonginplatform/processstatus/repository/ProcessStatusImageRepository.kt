package com.pluxity.yonginplatform.processstatus.repository

import com.pluxity.yonginplatform.processstatus.entity.ProcessStatusImage
import org.springframework.data.jpa.repository.JpaRepository

interface ProcessStatusImageRepository : JpaRepository<ProcessStatusImage, Long>
