package com.pluxity.yongin.keymanagement.entity

enum class KeyManagementType(
    val order: Int,
    val description: String,
) {
    QUALITY(1, "품질"),
    SAFETY(2, "안전"),
    METHOD(3, "공법"),
    CONSTRUCTION(4, "시공"),
    MATERIAL(5, "자재"),
    ;

    companion object {
        fun sortedEntries(): List<KeyManagementType> = entries.sortedBy { it.order }
    }
}
