import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") { enabled = true }

dependencies {
    implementation(project(":common:core"))
    implementation(project(":common:auth"))
    implementation(rootProject.libs.logbook)
    implementation(rootProject.libs.spring.boot.starter.webflux)

    testImplementation(project(":common:test-support"))
}
