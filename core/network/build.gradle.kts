plugins {
    id("vetro.android.library.plain")
    id("vetro.kotlin.serialization")
    alias(libs.plugins.apollo)
}

android {
    namespace = "com.example.myapplication.network"
}

apollo {
    service("anilist") {
        packageName.set("com.example.myapplication.network.anilist")
        introspection {
            endpointUrl.set("https://graphql.anilist.co")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}

dependencies {
    api(platform(libs.ktor.bom))
    api(libs.ktor.client.core)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.kotlinx.serialization.json)
    api("com.apollographql.apollo:apollo-runtime:4.0.0")
    api("com.apollographql.apollo:apollo-engine-ktor:4.0.0")
}
