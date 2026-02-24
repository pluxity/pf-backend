// common:auth — JWT, Security, User, Role, Permission

dependencies {
    api(project(":common:core"))

    api(rootProject.libs.spring.boot.starter.security)
    implementation(rootProject.libs.spring.boot.starter.data.redis)
    implementation(rootProject.libs.bundles.jjwt)

    testImplementation(testFixtures(project(":common:core")))
    testImplementation(rootProject.libs.spring.security.test)
}