// apps:gs-auth — 기존 plug-platform-api-kotlin (gs + core)
// 도메인: facility, asset, category, feature, label3d, patrol, cctv

import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") { enabled = true }

dependencies {
    implementation(project(":common:core"))
    implementation(project(":common:auth"))
    implementation(project(":common:file"))
    implementation(project(":common:messaging"))

    implementation(rootProject.libs.spring.boot.starter.webflux)
    implementation(rootProject.libs.bundles.coroutines)
    implementation(rootProject.libs.spring.boot.starter.actuator)
    implementation(rootProject.libs.micrometer.prometheus)
    implementation(rootProject.libs.logbook)

    // Flyway
    implementation(rootProject.libs.flyway.core)
    runtimeOnly(rootProject.libs.flyway.postgresql)

    // Cron (patrol scheduler)
    implementation(rootProject.libs.cron.utils)

    testImplementation(project(":common:test-support"))
}
