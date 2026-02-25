package com.pluxity.yongin.processstatus.repository

import com.pluxity.yongin.processstatus.entity.ProcessStatusImage
import org.springframework.data.jpa.repository.JpaRepository

interface ProcessStatusImageRepository : JpaRepository<ProcessStatusImage, Long>
