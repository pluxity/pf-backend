package com.pluxity.safers.cctv.repository

import com.pluxity.safers.cctv.entity.Cctv
import com.pluxity.safers.llm.dto.CctvFilterCriteria

interface CctvCustomRepository {
    fun findAllWithSite(criteria: CctvFilterCriteria? = null): List<Cctv>
}
