package com.pluxity.common.messaging.dto

import java.security.Principal
import java.util.UUID

class StompPrincipal(
    private val name: String = UUID.randomUUID().toString(),
) : Principal {
    override fun getName(): String = name
}
