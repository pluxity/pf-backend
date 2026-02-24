// common:file — StorageStrategy, S3, Local file upload

dependencies {
    api(project(":common:core"))

    implementation(rootProject.libs.aws.s3)

    testImplementation(testFixtures(project(":common:core")))
}
