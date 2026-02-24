// common:core — BaseEntity, ErrorCode, Exception, Response, AOP, Utils
// 모든 모듈이 의존하는 인프라 기반

plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(rootProject.libs.logbook)
    // AuditorAwareImpl uses SecurityContextHolder
    implementation(rootProject.libs.spring.boot.starter.security)

    testFixturesImplementation(rootProject.libs.spring.boot.starter.test)
}
