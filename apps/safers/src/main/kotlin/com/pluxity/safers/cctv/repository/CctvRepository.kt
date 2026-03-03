package com.pluxity.safers.cctv.repository

import com.pluxity.safers.cctv.entity.Cctv
import org.springframework.data.jpa.repository.JpaRepository

interface CctvRepository : JpaRepository<Cctv, Long>
