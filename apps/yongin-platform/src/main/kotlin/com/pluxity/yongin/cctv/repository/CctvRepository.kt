package com.pluxity.yongin.cctv.repository

import com.pluxity.yongin.cctv.entity.Cctv
import org.springframework.data.jpa.repository.JpaRepository

interface CctvRepository : JpaRepository<Cctv, Long>
