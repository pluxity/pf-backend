// shared:cctv — CCTV 도메인 (entity, repository, service, controller, client, dto)
// 여러 앱에서 공유하는 CCTV 모듈

dependencies {
    api(project(":common:core"))

    implementation(rootProject.libs.spring.boot.starter.webflux)

    testImplementation(testFixtures(project(":common:core")))
}
