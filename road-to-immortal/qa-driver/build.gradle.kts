plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// An isolated test host keeps the consumer APK uninstrumented and fully optimized.
android {
    namespace = "id.wynn.roadtoimmortal.qa"
    compileSdk = 36
    defaultConfig {
        applicationId = "id.wynn.roadtoimmortal.qa"
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
