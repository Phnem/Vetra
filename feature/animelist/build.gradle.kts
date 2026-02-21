plugins {
    id("vetro.android.library")
    id("vetro.compose")
}

android {
    namespace = "com.example.myapplication.feature.animelist"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")
    
    // Koin
    implementation("io.insert-koin:koin-androidx-compose:3.5.6")
}
