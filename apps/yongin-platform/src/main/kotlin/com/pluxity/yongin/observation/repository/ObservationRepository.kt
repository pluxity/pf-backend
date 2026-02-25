package com.pluxity.yongin.observation.repository

import com.pluxity.yongin.observation.entity.Observation
import org.springframework.data.jpa.repository.JpaRepository

interface ObservationRepository : JpaRepository<Observation, Long>
