// common:test-support — DummyEntities, DummyDTOs, base test configurations

dependencies {
    api(project(":common:core"))
    api(testFixtures(project(":common:core")))
    api(project(":common:auth"))
    api(project(":common:file"))

    api(rootProject.libs.spring.boot.starter.test)
    api(rootProject.libs.spring.boot.starter.webmvc.test)
    api(rootProject.libs.bundles.kotest)
    api(rootProject.libs.mockk)
    api(rootProject.libs.springmockk)
    api(rootProject.libs.spring.security.test)
    api(rootProject.libs.h2)
}
