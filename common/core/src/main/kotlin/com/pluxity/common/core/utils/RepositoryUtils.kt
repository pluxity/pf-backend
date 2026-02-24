package com.pluxity.common.core.utils

import com.pluxity.common.core.constant.Code
import com.pluxity.common.core.exception.CustomException

fun <T> findAllByIdsOrThrow(
    ids: List<Long>,
    findAllById: (List<Long>) -> List<T>,
    idExtractor: (T) -> Long,
    errorCode: Code,
): Map<Long, T> {
    if (ids.isEmpty()) return emptyMap()

    val entities = findAllById(ids)
    val foundIds = entities.map(idExtractor).toSet()
    val notFoundIds = ids.filter { it !in foundIds }
    if (notFoundIds.isNotEmpty()) {
        throw CustomException(errorCode, notFoundIds)
    }
    return entities.associateBy(idExtractor)
}
