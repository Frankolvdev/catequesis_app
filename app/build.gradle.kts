plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.chayzay.catequesisapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.chayzay.catequesisapp"
        minSdk = 23
        targetSdk = 36
        // Confirm the highest code in Play Console before publishing.
        versionCode = 7
        versionName = "2.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // X API v2: define X_BEARER_TOKEN in ~/.gradle/gradle.properties or project gradle.properties.
        // It is intentionally empty until the production X credentials are supplied.
        buildConfigField("String", "X_BEARER_TOKEN", "\"${project.findProperty("X_BEARER_TOKEN") ?: ""}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${project.findProperty("GOOGLE_WEB_CLIENT_ID") ?: "64342706407-o3nic72jjc2a37ojfvfe2c7r0dosl011.apps.googleusercontent.com"}\"")
        buildConfigField("String", "FACEBOOK_APP_ID", "\"${project.findProperty("FACEBOOK_APP_ID") ?: "1463167569060482"}\"")
        buildConfigField("String", "FACEBOOK_CLIENT_TOKEN", "\"${project.findProperty("FACEBOOK_CLIENT_TOKEN") ?: "daa32d7fd32b8db9d8334a804588e4f4"}\"")
        resValue("string", "facebook_app_id", "${project.findProperty("FACEBOOK_APP_ID") ?: "1463167569060482"}")
        resValue("string", "fb_login_protocol_scheme", "fb${project.findProperty("FACEBOOK_APP_ID") ?: "1463167569060482"}")
        resValue("string", "facebook_client_token", "${project.findProperty("FACEBOOK_CLIENT_TOKEN") ?: "daa32d7fd32b8db9d8334a804588e4f4"}")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation("androidx.compose.animation:animation-core")
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.facebook.android:facebook-login:18.3.0")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-database")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
