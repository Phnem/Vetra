plugins {
    id("vetro.android.library")
    id("vetro.sqldelight")
    id("vetro.kotlin.serialization")
}

android {
    namespace = "com.example.myapplication.database"
}

dependencies {
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.coroutines)
    implementation(libs.kotlinx.serialization.json)
}
