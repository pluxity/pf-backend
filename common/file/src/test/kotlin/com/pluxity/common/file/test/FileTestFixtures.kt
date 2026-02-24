package com.pluxity.common.file.test

import com.pluxity.common.core.test.withAudit
import com.pluxity.common.core.test.withId
import com.pluxity.common.file.entity.FileEntity

fun dummyFileEntity(
    id: Long? = 1L,
    filePath: String = "temp/test-file.png",
    originalFileName: String = "test.png",
    contentType: String = "image/png",
): FileEntity =
    FileEntity(
        filePath = filePath,
        originalFileName = originalFileName,
        contentType = contentType,
    ).withAudit().withId(id)
