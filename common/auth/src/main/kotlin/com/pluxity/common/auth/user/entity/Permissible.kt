package com.pluxity.common.auth.user.entity

interface Permissible {
    val resourceId: String

    val resourceType: String
}
