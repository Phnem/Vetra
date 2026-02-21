import org.gradle.kotlin.dsl.configure
import app.cash.sqldelight.gradle.SqlDelightExtension

plugins {
    id("app.cash.sqldelight")
}

extensions.configure<SqlDelightExtension> {
    databases {
        create("VetroDatabase") {
            packageName.set("com.example.myapplication.data.local")
        }
    }
}
