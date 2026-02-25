// apps:yongin-platform — 기존 plug-siteguard-api
// 도메인: attendance, goal, keymanagement, notice, observation, processstatus, safetyequipment, systemsetting

import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") { enabled = true }

dependencies {
    implementation(project(":common:core"))
    implementation(project(":common:auth"))
    implementation(project(":common:file"))
    implementation(project(":common:messaging"))
    implementation(project(":shared:cctv"))

    implementation(rootProject.libs.spring.boot.starter.webflux)
    implementation(rootProject.libs.spring.boot.starter.websocket)
    implementation(rootProject.libs.bundles.springwolf)
    implementation(rootProject.libs.bundles.coroutines)
    implementation(rootProject.libs.logbook)

    testImplementation(project(":common:test-support"))
}
