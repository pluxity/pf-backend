package com.pluxity.yonginplatform.safetyequipment.repository

import com.pluxity.yonginplatform.safetyequipment.entity.SafetyEquipment
import org.springframework.data.jpa.repository.JpaRepository

interface SafetyEquipmentRepository : JpaRepository<SafetyEquipment, Long>
