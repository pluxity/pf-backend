package com.pluxity.yonginplatform.safetyequipment.entity

import com.pluxity.common.core.entity.IdentityIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "safety_equipments")
class SafetyEquipment(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "quantity", nullable = false)
    var quantity: Int,
) : IdentityIdEntity() {
    fun update(
        name: String,
        quantity: Int,
    ) {
        this.name = name
        this.quantity = quantity
    }
}
