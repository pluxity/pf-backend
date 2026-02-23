package com.pluxity.common.core.response

import com.pluxity.common.core.constant.SuccessCode

class DataResponseBody<T>(
    val data: T?,
) : ResponseBody(SuccessCode.SUCCESS.getHttpStatus().value(), SuccessCode.SUCCESS.getMessage())
