package com.pluxity.yonginplatform.cctv.repository

import com.pluxity.yonginplatform.cctv.entity.Cctv
import org.springframework.data.jpa.repository.JpaRepository

interface CctvRepository : JpaRepository<Cctv, Long>
