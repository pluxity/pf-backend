package com.pluxity.yonginplatform.observation.repository

import com.pluxity.yonginplatform.observation.entity.Observation
import org.springframework.data.jpa.repository.JpaRepository

interface ObservationRepository : JpaRepository<Observation, Long>
