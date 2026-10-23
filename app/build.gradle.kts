plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releaseStoreFilePath = providers.gradleProperty("ANDROID_RELEASE_STORE_FILE").orNull ?: System.getenv("ANDROID_RELEASE_STORE_FILE")
val releaseStorePasswordValue = providers.gradleProperty("ANDROID_RELEASE_STORE_PASSWORD").orNull ?: System.getenv("ANDROID_RELEASE_STORE_PASSWORD")
val releaseKeyAliasValue = providers.gradleProperty("ANDROID_RELEASE_KEY_ALIAS").orNull ?: System.getenv("ANDROID_RELEASE_KEY_ALIAS")
val releaseKeyPasswordValue = providers.gradleProperty("ANDROID_RELEASE_KEY_PASSWORD").orNull ?: System.getenv("ANDROID_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFilePath, releaseStorePasswordValue, releaseKeyAliasValue, releaseKeyPasswordValue).all { !it.isNullOrBlank() }

android {
    namespace = "dev.techda.sahatisukari"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.techda.sahatisukari"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.2.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"https://sahatisukarie.up.railway.app/api/v1/\"")
    }
    signingConfigs {
        if (hasReleaseSigning) create("production") {
            storeFile = file(releaseStoreFilePath!!)
            storePassword = releaseStorePasswordValue
            keyAlias = releaseKeyAliasValue
            keyPassword = releaseKeyPasswordValue
        }
    }
    buildTypes {
        getByName("release") {
            isDebuggable = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("production")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

ksp { arg("dagger.fastInit", "enabled") }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation(libs.androidx.compose.ui.tooling)
}
