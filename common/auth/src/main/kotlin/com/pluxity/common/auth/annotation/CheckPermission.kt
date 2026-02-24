package com.pluxity.common.auth.annotation

import com.pluxity.common.auth.permission.PermissionLevel
import com.pluxity.common.auth.user.entity.PermissionAction

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CheckPermission(
    val action: PermissionAction = PermissionAction.READ_SINGLE,
    val resourceType: String = "",
    val level: PermissionLevel = PermissionLevel.READ,
    val idParamIndex: Int = 0,
)
