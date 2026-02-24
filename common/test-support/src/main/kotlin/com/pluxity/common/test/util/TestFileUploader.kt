package com.pluxity.common.test.util

import com.pluxity.common.file.service.FileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class TestFileUploader {
    @Autowired
    lateinit var fileService: FileService

    fun initiateTestFileUpload(filename: String): Long {
        val mockFile: MultipartFile =
            MockMultipartFile(
                "file",
                filename,
                "image/png",
                "test-image-content".toByteArray(),
            )
        return fileService.initiateUpload(mockFile)
    }
}
