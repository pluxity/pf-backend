// common:auth — JWT, Security, User, Role, Permission

dependencies {
    api(project(":common:core"))
    implementation(project(":common:file"))

    api(rootProject.libs.spring.boot.starter.security)
    implementation(rootProject.libs.spring.boot.starter.data.redis)
    implementation("com.nimbusds:nimbus-jose-jwt:10.3")

    testImplementation(project(":common:test-support"))
    testImplementation(testFixtures(project(":common:core")))
}