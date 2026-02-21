plugins {
    id("vetro.android.library")
    id("vetro.compose")
}

android {
    namespace = "com.example.myapplication.feature.statistics"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
}
