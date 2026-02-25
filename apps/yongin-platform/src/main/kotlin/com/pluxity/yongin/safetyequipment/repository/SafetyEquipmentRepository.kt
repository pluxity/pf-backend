package com.pluxity.yongin.safetyequipment.repository

import com.pluxity.yongin.safetyequipment.entity.SafetyEquipment
import org.springframework.data.jpa.repository.JpaRepository

interface SafetyEquipmentRepository : JpaRepository<SafetyEquipment, Long>
