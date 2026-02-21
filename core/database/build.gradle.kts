plugins {
    id("vetro.android.library")
    id("vetro.sqldelight")
    id("vetro.kotlin.serialization")
}

android {
    namespace = "com.example.myapplication.database"
}

dependencies {
    implementation("app.cash.sqldelight:android-driver:2.0.2")
    implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
