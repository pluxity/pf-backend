package com.pluxity.common.test.dto

import com.pluxity.common.file.dto.FileResponse

fun dummyFileResponse(
    id: Long? = 1L,
    url: String? = "https://example.com/files/test.jpg",
    originalFileName: String? = "test.jpg",
    contentType: String? = "image/jpeg",
    fileStatus: String? = "COMPLETE",
) = FileResponse(
    id = id,
    url = url,
    originalFileName = originalFileName,
    contentType = contentType,
    fileStatus = fileStatus,
    baseResponse = dummyBaseResponse(),
)
