// apps:safers-collect — 현장 수집 모듈 (게이트웨이)
// 다른 사내 모듈에 의존하지 않는 독립 Spring Boot 앱.
// 본 단계에서는 API 껍데기 + Swagger 만 제공.

import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") { enabled = true }

// 루트 subprojects 블록이 강제하는 JPA/Flyway/Postgres/p6spy 는
// 본 모듈에서는 사용하지 않으므로 컴파일 클래스패스에서 제외.
configurations.all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-data-jpa")
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-flyway")
    exclude(group = "org.flywaydb", module = "flyway-database-postgresql")
    exclude(group = "org.postgresql", module = "postgresql")
    exclude(group = "com.github.gavlyukovskiy", module = "p6spy-spring-boot-starter")
}

dependencies {
    implementation(rootProject.libs.spring.boot.starter.webflux)
}
