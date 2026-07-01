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

}
