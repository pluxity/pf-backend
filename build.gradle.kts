import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spotless)
}

allprojects {
    group = "com.pluxity"
    version = "1.0.0"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.diffplug.spotless")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    // ── Common Dependencies (all subprojects) ──
    dependencies {
        implementation(rootProject.libs.spring.boot.starter)
        implementation(rootProject.libs.spring.boot.starter.web)
        implementation(rootProject.libs.spring.boot.starter.data.jpa)
        implementation(rootProject.libs.spring.boot.starter.validation)
        implementation(rootProject.libs.kotlin.reflect)
        implementation(rootProject.libs.jackson.module.kotlin)
        implementation(rootProject.libs.kotlin.logging)
        implementation(rootProject.libs.bundles.kotlin.jdsl)
        implementation(rootProject.libs.bundles.springdoc)
        implementation(rootProject.libs.p6spy)

        runtimeOnly(rootProject.libs.postgresql)

        testImplementation(rootProject.libs.spring.boot.starter.test)
        testImplementation(rootProject.libs.kotlin.test.junit5)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
        testImplementation(rootProject.libs.bundles.kotest)
        testImplementation(rootProject.libs.mockk)
        testImplementation(rootProject.libs.h2)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // ── Default: library module (no bootJar) ──
    tasks.withType<BootJar> {
        enabled = false
    }

    tasks.jar {
        enabled = true
    }

    // ── Spotless / ktlint ──
    spotless {
        kotlin {
            target("src/**/*.kt")
            ktlint()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
