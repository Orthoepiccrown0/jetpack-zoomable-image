plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

group = "com.epiccrown"
version = "0.1.0"

android {
    namespace = "com.epiccrown.zoomableimage"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.epiccrown"
            artifactId = "zoomable-image"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            val configuredUrl = providers.gradleProperty("zoomableImage.githubPackagesUrl")
            val repository = providers.gradleProperty("gpr.repository")
                .orElse(providers.environmentVariable("GITHUB_REPOSITORY"))
            url = uri(
                configuredUrl.orNull
                    ?: "https://maven.pkg.github.com/${repository.orNull ?: "Orthoepiccrown0/jetpack-zoomable-image"}"
            )
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}
