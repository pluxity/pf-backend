// common:messaging — WebSocket, STOMP, SessionManager

dependencies {
    api(project(":common:core"))

    implementation(rootProject.libs.spring.boot.starter.websocket)
    implementation(rootProject.libs.bundles.springwolf)
    runtimeOnly(rootProject.libs.springwolf.ui)
}
