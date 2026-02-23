package com.pluxity.common.core.exception

import com.pluxity.common.core.constant.Code

class CustomException(
    val code: Code,
    @Transient vararg val params: Any?,
) : RuntimeException(formatMessage(code, params)) {
    companion object {
        private fun formatMessage(
            code: Code,
            params: Array<out Any?>,
        ): String {
            if (params.isEmpty()) return code.getMessage()
            return try {
                String.format(code.getMessage(), *params)
            } catch (_: Exception) {
                "${code.getMessage()} (format params: ${params.contentToString()})"
            }
        }
    }
}
