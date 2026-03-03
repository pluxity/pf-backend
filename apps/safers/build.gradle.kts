// apps:safers — 기존 safers-api
// 도메인: site(Spatial), event, weather, configuration

import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") { enabled = true }

dependencies {
    implementation(project(":common:core"))
    implementation(project(":common:auth"))
    implementation(project(":common:file"))
    implementation(project(":common:messaging"))
    implementation(rootProject.libs.spring.boot.starter.webflux)
    implementation(rootProject.libs.spring.boot.starter.websocket)
    implementation(rootProject.libs.bundles.springwolf)
    implementation(rootProject.libs.bundles.coroutines)
    implementation(rootProject.libs.logbook)

    // Hibernate Spatial + JTS (Site polygon)
    implementation(rootProject.libs.bundles.spatial)

    testImplementation(project(":common:test-support"))
}
