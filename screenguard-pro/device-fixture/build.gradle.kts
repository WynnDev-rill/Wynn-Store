plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "id.wynn.guardfixture"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig { applicationId = "id.wynn.guardfixture"; minSdk = 30; targetSdk = 36; versionCode = 1; versionName = "test-only" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
