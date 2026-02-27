// common:auth — JWT, Security, User, Role, Permission

dependencies {
    api(project(":common:core"))

    api(rootProject.libs.spring.boot.starter.security)
    implementation(rootProject.libs.spring.boot.starter.data.redis)
    implementation("com.nimbusds:nimbus-jose-jwt:10.3")

    testImplementation(testFixtures(project(":common:core")))
    testImplementation(rootProject.libs.spring.security.test)
}