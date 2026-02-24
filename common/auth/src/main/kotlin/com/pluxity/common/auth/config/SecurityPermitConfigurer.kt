package com.pluxity.common.auth.config

/**
 * 앱별 Security permitAll 경로를 추가하기 위한 인터페이스.
 * 각 앱에서 @Configuration으로 구현하면 CommonSecurityConfig에서 자동으로 반영됩니다.
 */
interface SecurityPermitConfigurer {
    /** 추가로 permitAll 처리할 경로 목록 */
    fun permitPaths(): List<String> = emptyList()
}
