import org.gradle.kotlin.dsl.dependencies

dependencies {
    add("implementation", platform("androidx.compose:compose-bom:2024.12.01"))
    add("implementation", "androidx.compose.ui:ui")
    add("implementation", "androidx.compose.ui:ui-graphics")
    add("implementation", "androidx.compose.ui:ui-tooling-preview")
    add("implementation", "androidx.compose.material3:material3")
    add("implementation", "androidx.compose.material:material-icons-extended")
    add("implementation", "androidx.compose.animation:animation")
    add("debugImplementation", "androidx.compose.ui:ui-tooling")
    add("androidTestImplementation", platform("androidx.compose:compose-bom:2024.12.01"))
    add("androidTestImplementation", "androidx.compose.ui:ui-test-junit4")
}
