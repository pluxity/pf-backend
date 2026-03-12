package com.pluxity.weekly.chat.action

inline fun <reified T : Enum<T>> parseEnum(value: String): T? =
    try {
        enumValueOf<T>(value.uppercase().replace(" ", "_"))
    } catch (_: IllegalArgumentException) {
        null
    }
